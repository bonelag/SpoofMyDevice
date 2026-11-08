# Điều tra treo logo và bản sửa 2.0.4

> Cập nhật 2026-11-06: bản sửa khởi tạo hiện tại đã được cài và kiểm tra qua ADB trên V2507A, KernelSU v3.3.1, LSPosed 2.2.1. Ba lần soft reboot với hồ sơ SDK 35/36/37 đều vào hệ thống, module vẫn bật và không có Boot Guard. Xem [báo cáo kiểm tra thiết bị](boot-safety-validation-2026-11-06.md). Các kết quả và giới hạn bên dưới thuộc lần điều tra lịch sử 2.0.4.

Đã loại bỏ các đường đọc cấu hình và chờ subprocess ngay trên luồng khởi tạo ứng dụng. Chưa có log hoặc thử nghiệm trên thiết bị để xác nhận nguyên nhân cuối cùng của màn hình đen.

## Phạm vi và bằng chứng

Thiết bị được báo lỗi: iQOO Z10 Turbo Plus, root tạm thời bằng KernelSU, soft reboot, LSPosed 2.2.1. Scope chỉ gồm ứng dụng thường. Thiết bị chưa thể kết nối ADB trong lần sửa này.

Gói được cung cấp: `LSPosed-v2.2.1.zip`.

```text
ZIP SHA-256:
ED37DE2D2097AA6BA871377E7BF4AC719B708484F9468928E6ABAE10AB311E74

framework.dex SHA-256:
73CCEB77D02950CF7F224900F23A896506A0164AB7008E1430E502FF985D0140
```

Tái sử dụng kết quả giải nén bằng 7-Zip và giải mã bằng JADX CLI 1.5.6 trong `build/boot-investigation/`. Các lần gọi REA đã lưu trong thư mục này không thành công: ZIP dùng phương thức nén chưa được hỗ trợ, provider Android thiếu `jadx-headless-mcp`, và provider ELF yêu cầu Linux. Chúng không cấp Evidence ID và không được dùng như bằng chứng thực thi.

Quan sát tĩnh từ `framework-source/sources/`:

| Quan sát | Vị trí |
| --- | --- |
| Callback legacy được gọi trong hook của `LoadedApk`, sau `chain.proceed()`. | `defpackage/f1.java:87` |
| `isFirstApplication` chỉ đúng khi tên gói hiện tại khớp gói đang nạp và `ActivityThread` đã có tên gói, tên tiến trình. | `defpackage/f1.java:43`, `:46`, `:92` |
| `initZygote` chỉ được gọi cho đối tượng thực hiện `IXposedHookZygoteInit`; SMD chỉ thực hiện `IXposedHookLoadPackage`. | `de/robv/android/xposed/XposedInit.java:172`; mã SMD `MainHook.java` |
| Constructor `XSharedPreferences` gọi `initialize()`, rồi đọc file đồng bộ qua `finishLoadLocked()` và `readLoadedState()`. Getter cũng có đường `awaitLoadedLocked()`. | `de/robv/android/xposed/XSharedPreferences.java:138`, `:207`, `:235`, `:310`, `:723` |
| Script `emulated-soft-reboot.sh` gọi `lspctl stop`; script này không đủ để xác định cách ROM reset mọi cờ boot. | `lsposed-2.2.1/emulated-soft-reboot.sh` |

Các quan sát này không chứng minh Zygote crash trên thiết bị. Việc thanh thông báo còn hoạt động cũng không đủ để xác định thành phần bị lỗi. Chưa phân tích được toàn bộ mã native hoặc luồng reboot của ROM.

## Đường có thể gây chờ trong SMD

Trước bản sửa, việc nạp hồ sơ chạy trong callback khởi tạo và `Application.attach`. Constructor/preferences, đọc file, ContentProvider và broadcast có thể làm luồng gọi phải chờ. Getter cấu hình còn có thể tự reload khi được gọi từ hook. Hook subprocess đọc hết stdout, stderr và chờ tiến trình trước khi trả `Process` cho ứng dụng; ứng dụng chưa nhận được process để tiêu thụ hai stream song song hoặc hủy nó.

Đây là các đường chặn có trong mã nguồn. Mối liên hệ trực tiếp giữa chúng với treo logo trên iQOO vẫn cần log hoặc thử nghiệm thiết bị.

## Thay đổi trong 2.0.4

- Đọc cờ boot thật mỗi lần, bỏ cache giá trị `true`. Yêu cầu `sys.boot_completed=1` và `init.svc.bootanim=stopped`; trạng thái thiếu hoặc không rõ giữ giá trị thật.
- Không tạo worker, đọc hồ sơ hoặc cài hook khởi tạo cho ứng dụng bắt đầu trong thời gian boot. Cần force stop rồi mở lại ứng dụng sau boot để áp dụng spoofing.
- Bỏ qua UID hệ thống, UID isolated/sandbox, ứng dụng hệ thống hoặc cập nhật ứng dụng hệ thống, gói được bảo vệ và callback của gói phụ trong tiến trình khác.
- Worker nạp hồ sơ; getter hook chỉ đọc cấu hình đã công bố trong bộ nhớ. Nhiều yêu cầu reload được gộp, không tạo hàng đợi tăng theo từng broadcast.
- Phân biệt dữ liệu không đọc được với cấu hình hợp lệ không còn gán hồ sơ. Lỗi hoặc JSON chưa hoàn chỉnh giữ hồ sơ trước; bỏ gán hợp lệ xóa hồ sơ runtime. Provider có phương thức `get_state` để giữ được phân biệt này.
- Ghi nhận Build thật trước khi cài hook SystemProperties; áp dụng Build sau khi cài các nhóm hook khác. Build được khôi phục khi bỏ gán hồ sơ. Quyết định đăng ký receiver dùng SDK thật.
- Trả process ngay; chỉ đổi stdout khi ứng dụng đọc. Đọc theo dòng, giới hạn bộ đệm dòng 64 KiB, giữ stderr, stdin, mã thoát và thao tác hủy. Dòng quá dài được trả nguyên văn. Lệnh shell phức hợp và `cat` nhiều file không bị thay output.
- Cấu hình và getprop dump không được thay các thuộc tính điều khiển boot như `sys.boot_completed`, `init.svc.bootanim`, `sys.powerctl`.

Nạp bất đồng bộ có giới hạn: ứng dụng có thể đọc hoặc cache giá trị thật trong cold start trước khi hồ sơ sẵn sàng. Những giá trị ứng dụng đã cache có thể cần force stop để kiểm tra lại. Bản sửa ưu tiên tránh chặn khởi tạo.

## Kiểm tra trên thiết bị và Boot Guard

1. Giữ Boot Guard cài sẵn khi cập nhật APK 2.0.4. Sau khi hệ thống khởi động xong, bật SMD, giữ scope ứng dụng thường, gán hồ sơ rồi force stop và mở lại ứng dụng đích. Kiểm tra model, Android ID và Safe Mode.
2. Khi có điều kiện kiểm tra và có thể dùng cách khôi phục đã áp dụng trước đây, tạm tắt Boot Guard. Giữ SMD bật rồi thực hiện soft reboot. Nếu Boot Guard vẫn tự tắt SMD, lần reboot đó chưa kiểm tra được bản sửa.
3. Chỉ cân nhắc gỡ Boot Guard sau khi vài lần soft reboot liên tiếp vào hệ thống bình thường, màn hình và ứng dụng hoạt động, SMD vẫn bật sau reboot. Bản sửa trên máy tính chưa đủ để kết luận có thể gỡ ngay.
4. Nếu lỗi lặp lại, lấy logcat và log LSPosed khi có thể kết nối ADB. Cần xác định tiến trình crash/ANR, trạng thái launcher/SystemUI và stack chờ trước khi quy lỗi cho Zygote.

Các lệnh đọc trạng thái khi ADB đã kết nối:

```sh
adb shell getprop sys.boot_completed
adb shell getprop init.svc.bootanim
adb logcat -b crash -d
adb logcat -b all -v threadtime > smd-boot-logcat.txt
```

## Kiểm tra trên máy tính

```text
gradlew.bat --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Kiểm thử hồi quy bao gồm đọc cấu hình khi nguồn nạp bị chặn, lỗi nạp không xóa hồ sơ trước, bỏ gán không dùng mục preferences cũ, cờ boot trong soft reboot, UID/gói được bảo vệ, stdout đọc được khi process còn chạy, dòng dài không chờ EOF, và giữ mã thoát/stderr/hủy process.

Kết quả ngày 2026-11-05 (Asia/Saigon): 22 kiểm thử đạt, `assembleDebug` đạt, `lintDebug` có 0 lỗi và 119 cảnh báo. Đã bổ sung 36 nhãn còn thiếu cho mỗi ngôn ngữ Hàn, Nhật, Trung để xử lý lỗi `MissingTranslation`. `apksigner verify` xác nhận chữ ký v2 hợp lệ; APK là bản debug của `com.spoofmydevice`, versionCode 13, versionName 2.0.4.

APK dùng thử: `release/SpoofMyDevice-2.0.4-bootfix.apk`; checksum: file `.sha256` cạnh APK.

Kết quả kiểm thử trên JVM không thay thế thử nghiệm hook LSPosed, provider IPC hoặc soft reboot trên Android thật.
