#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <arpa/inet.h>
#include "sqlite3.h"

#define DEFAULT_DB "/data/adb/lspd/config/modules_config.db"
#define DEFAULT_PKG "com.spoofmydevice"

static void write_int(int fd, int val) {
    uint32_t net_val = htonl((uint32_t)val);
    write(fd, &net_val, 4);
}

static int read_int(int fd) {
    uint32_t net_val = 0;
    read(fd, &net_val, 4);
    return (int)ntohl(net_val);
}

static void read_string(int fd, char *buf, size_t max_len) {
    int len = read_int(fd);
    if (len < 0 || (size_t)len >= max_len) {
        buf[0] = '\0';
        return;
    }
    int total = 0;
    while (total < len) {
        int r = read(fd, buf + total, len - total);
        if (r <= 0) break;
        total += r;
    }
    buf[total] = '\0';
}

static int notify_lsposed_daemon(const char *pkg, int enable) {
    FILE *fp = fopen("/data/adb/lspd/monitor", "r");
    if (!fp) {
        return -1; // Daemon not running yet (normal in post-fs-data)
    }
    char sock_name[256];
    if (!fgets(sock_name, sizeof(sock_name), fp)) {
        fclose(fp);
        return -1;
    }
    fclose(fp);
    char *nl = strpbrk(sock_name, "\r\n");
    if (nl) *nl = '\0';

    int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd < 0) return -2;

    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    addr.sun_path[0] = '\0'; // abstract namespace
    strncpy(addr.sun_path + 1, sock_name, sizeof(addr.sun_path) - 2);

    socklen_t addr_len = sizeof(sa_family_t) + 1 + strlen(sock_name);
    if (connect(fd, (struct sockaddr *)&addr, addr_len) < 0) {
        close(fd);
        return -3;
    }

    // Command 5: CLI
    uint8_t cmd = 5;
    write(fd, &cmd, 1);
    write_int(fd, 1280528451); // 0x4C535043
    write_int(fd, 7912);       // version

    const char *args[5];
    args[0] = "module";
    args[1] = enable ? "enable" : "disable";
    args[2] = pkg;
    args[3] = "--user";
    args[4] = "0";

    write_int(fd, 5);
    for (int i = 0; i < 5; i++) {
        int len = strlen(args[i]);
        write_int(fd, len);
        write(fd, args[i], len);
    }

    int resp_magic = read_int(fd);
    int resp_ver = read_int(fd);
    int exit_code = read_int(fd);

    char stdout_buf[4096];
    char stderr_buf[4096];
    char json_buf[4096];
    read_string(fd, stdout_buf, sizeof(stdout_buf));
    read_string(fd, stderr_buf, sizeof(stderr_buf));
    read_string(fd, json_buf, sizeof(json_buf));

    if (exit_code == 0) {
        printf("[SMD-Guard] Daemon notified via socket: module %s %s successful\n",
               args[1], pkg);
    } else {
        fprintf(stderr, "[SMD-Guard] Daemon socket response (exit %d): %s %s\n",
                exit_code, stdout_buf, stderr_buf);
    }

    close(fd);
    return exit_code;
}

int main(int argc, char **argv) {
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <0|1> [pkg] [db_path]\n", argv[0]);
        return 1;
    }

    int enable = atoi(argv[1]);
    const char *pkg = (argc >= 3 && strlen(argv[2]) > 0) ? argv[2] : DEFAULT_PKG;
    const char *db_path = (argc >= 4 && strlen(argv[3]) > 0) ? argv[3] : DEFAULT_DB;

    // 1. Update SQLite DB directly
    sqlite3 *db;
    int rc = sqlite3_open(db_path, &db);
    if (rc == SQLITE_OK) {
        sqlite3_busy_timeout(db, 3000);

        char sql[512];
        snprintf(sql, sizeof(sql),
                 "UPDATE modules_state SET enabled = %d WHERE module_pkg_name = '%s';",
                 enable, pkg);

        char *errmsg = NULL;
        rc = sqlite3_exec(db, sql, NULL, NULL, &errmsg);
        if (rc == SQLITE_OK) {
            printf("[SMD-Guard] Set %s enabled=%d (rows: %d) in DB %s\n",
                   pkg, enable, sqlite3_changes(db), db_path);
        } else {
            fprintf(stderr, "[SMD-Guard] SQL error: %s\n", errmsg ? errmsg : "unknown");
            if (errmsg) sqlite3_free(errmsg);
        }
        sqlite3_close(db);
    } else {
        fprintf(stderr, "[SMD-Guard] Failed to open DB %s: %s\n", db_path, sqlite3_errmsg(db));
        if (db) sqlite3_close(db);
    }

    // 2. Notify LSPosed daemon via live socket if running
    int daemon_rc = notify_lsposed_daemon(pkg, enable);
    if (daemon_rc == -1) {
        printf("[SMD-Guard] Daemon not running (normal during boot post-fs-data)\n");
    }

    return 0;
}
