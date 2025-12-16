Tóm tắt ưu tiên refactor hướng đối tượng

Mục tiêu ngắn gọn:
- Giảm coupling giữa UI và model.
- Làm selection logic ổn định, id-based và dễ unit-test.
- Tạo ranh giới rõ ràng (controller/manager, model, view).

Gợi ý hành động (theo thứ tự ưu tiên):

1) Extract `SelectionManager`
- Mục đích: quản lý trạng thái selection (id), listeners và khôi phục selection khi model thay đổi.
- Interface gợi ý: `getSelectedId(): IntegerProperty`, `select(id)`, `clear()`, `addListener(...)`.
- Lý do: tách concern cho dễ kiểm thử và tránh logic selection bị rải rác trong `JavaFXApp`.

2) Introduce `OrganismSnapshot` (immutable DTO)
- Khi hiển thị ListView hoặc chi tiết, truyền snapshot thay vì giữ tham chiếu model trực tiếp.
- Snapshot chứa immutable fields: `id`, `labelId`, `type`, `x`, `y`, `health`...
- Lợi ích: UI an toàn trước thay đổi model trong background (concurrency) và dễ mock trong test.

3) Make `Grid` emit fine-grained change events (observer pattern)
- Hiện tại `Grid` trả về toàn bộ list; nên thêm events: `organismAdded`, `organismRemoved`, `organismMoved`, `organismUpdated`.
- UI có thể subscribe để cập nhật chỉ phần cần thay đổi (performance + no flicker).

4) Move rendering/draw logic into `GridRenderer` or `CanvasView`
- `JavaFXApp` hiện chứa nhiều code vẽ; tách ra giúp test phần render và giảm kích thước lớp UI.

5) Centralize Model APIs and reduce direct access from UI
- Expose limited read-only model APIs (e.g., `getOrganismById`, `getSnapshots()`), tránh UI thao tác trực tiếp trên model collections.

6) Add unit tests for `Grid` and `SelectionManager`
- Mô phỏng việc thêm/xóa/thay thế organism và assert `SelectionManager` vẫn giữ selection theo id.

7) Consider command/event queue for simulation ticks
- Tách việc thay đổi model khỏi UI thread; simulation emits events consumed by model updaters on JavaFX thread or via Platform.runLater.

Kết luận & bước tiếp theo đề xuất (thực hiện ngay nếu muốn):
- Tôi có thể: (A) extract `SelectionManager` và thay `JavaFXApp` dùng nó, (B) tạo `OrganismSnapshot` và chuyển ListView sang dùng snapshot.
- Nếu bạn muốn tôi làm luôn, chọn 1 trong 2 hoặc cả hai; tôi sẽ áp dụng thay đổi nhỏ, chạy kiểm tra biên dịch và commit patch.
