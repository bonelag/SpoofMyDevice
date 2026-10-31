#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "sqlite3.h"

#define DEFAULT_DB "/data/adb/lspd/config/modules_config.db"
#define DEFAULT_PKG "com.spoofmydevice"

int main(int argc, char **argv) {
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <0|1> [pkg] [db_path]\n", argv[0]);
        return 1;
    }

    int enable = atoi(argv[1]);
    const char *pkg = (argc >= 3 && strlen(argv[2]) > 0) ? argv[2] : DEFAULT_PKG;
    const char *db_path = (argc >= 4 && strlen(argv[3]) > 0) ? argv[3] : DEFAULT_DB;

    sqlite3 *db;
    int rc = sqlite3_open(db_path, &db);
    if (rc != SQLITE_OK) {
        fprintf(stderr, "[SMD-Guard] Failed to open DB %s: %s\n", db_path, sqlite3_errmsg(db));
        if (db) sqlite3_close(db);
        return 2;
    }

    sqlite3_busy_timeout(db, 3000);

    char sql[512];
    snprintf(sql, sizeof(sql),
             "UPDATE modules_state SET enabled = %d WHERE module_pkg_name = '%s';",
             enable, pkg);

    char *errmsg = NULL;
    rc = sqlite3_exec(db, sql, NULL, NULL, &errmsg);
    if (rc != SQLITE_OK) {
        fprintf(stderr, "[SMD-Guard] SQL error: %s\n", errmsg ? errmsg : "unknown");
        if (errmsg) sqlite3_free(errmsg);
        sqlite3_close(db);
        return 3;
    }

    int changes = sqlite3_changes(db);
    printf("[SMD-Guard] Set %s enabled=%d (rows affected: %d) in %s\n", pkg, enable, changes, db_path);
    sqlite3_close(db);
    return 0;
}
