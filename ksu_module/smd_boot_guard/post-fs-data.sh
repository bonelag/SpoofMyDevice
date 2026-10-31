#!/system/bin/sh
MODDIR=${0%/*}
LOGFILE="/data/adb/smd_guard.log"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] [post-fs-data] SMD Guard triggered on boot" > "$LOGFILE"

chmod 755 "$MODDIR/bin/smd_lsp_toggle" 2>/dev/null
chmod 755 "$MODDIR/bin/sqlite3" 2>/dev/null

LSP_DB="/data/adb/lspd/config/modules_config.db"

if [ -f "$LSP_DB" ]; then
    "$MODDIR/bin/smd_lsp_toggle" 0 com.spoofmydevice "$LSP_DB" >> "$LOGFILE" 2>&1
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [post-fs-data] Disabled com.spoofmydevice in $LSP_DB" >> "$LOGFILE"
else
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] [post-fs-data] Database not found: $LSP_DB" >> "$LOGFILE"
fi
