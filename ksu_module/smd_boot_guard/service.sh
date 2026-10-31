#!/system/bin/sh
MODDIR=${0%/*}
LOGFILE="/data/adb/smd_guard.log"

# Wait for Android to signal boot complete
while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 2
done

echo "[$(date '+%Y-%m-%d %H:%M:%S')] [service] sys.boot_completed=1 detected" >> "$LOGFILE"

# Wait 6s buffer so OriginOS Launcher / SystemUI settle completely
sleep 6

LSP_DB="/data/adb/lspd/config/modules_config.db"

if [ -f "$LSP_DB" ]; then
    "$MODDIR/bin/smd_lsp_toggle" 1 com.spoofmydevice "$LSP_DB" >> "$LOGFILE" 2>&1
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [service] Re-enabled com.spoofmydevice in $LSP_DB" >> "$LOGFILE"
else
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [service] Database not found: $LSP_DB" >> "$LOGFILE"
fi
