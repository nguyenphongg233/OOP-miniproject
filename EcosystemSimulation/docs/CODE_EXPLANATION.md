# Giải thích mã nguồn — EcosystemSimulation

Tài liệu này tóm tắt cấu trúc và chức năng chính của project `EcosystemSimulation` để dễ quản lý.

**Project**: Một mô phỏng hệ sinh thái đơn giản (thực vật, động vật ăn cỏ, động vật ăn thịt) sử dụng Java + JavaFX.

**Các điểm chạy:**
- Chạy bằng class `Main` trong [EcosystemSimulation/src/ecosystem/Main.java](EcosystemSimulation/src/ecosystem/Main.java) hoặc trực tiếp bằng JavaFX `JavaFXApp` ([EcosystemSimulation/src/ecosystem/ui/JavaFXApp.java](EcosystemSimulation/src/ecosystem/ui/JavaFXApp.java)).

**Tổng quan cấu trúc:**
- `src/ecosystem` : entry, settings, logic.
- `src/ecosystem/models` : lớp model cho `Organism`, `Plant`, `Herbivore`, `Carnivore`, `Grid`, snapshot.
- `src/ecosystem/logic` : `SimulationEngine` (cơ chế tick/step).
- `src/ecosystem/ui` : giao diện JavaFX (`JavaFXApp`) và `SelectionManager`.

---

**File: Main.java**
- Vị trí: [EcosystemSimulation/src/ecosystem/Main.java](EcosystemSimulation/src/ecosystem/Main.java)
- Mục đích: entry point CLI/launcher; gọi `Application.launch(JavaFXApp.class, args)` để khởi chạy JavaFX.

**File: Settings.java**
- Vị trí: [EcosystemSimulation/src/ecosystem/Settings.java](EcosystemSimulation/src/ecosystem/Settings.java)
- Chứa các tham số cấu hình mặc định: kích thước lưới, số lượng khởi tạo, năng lượng, tốc độ sinh trưởng, chi phí di chuyển, ngưỡng sinh sản.
- Được truyền vào `SimulationEngine` và `Grid` để khởi tạo mô phỏng.

**File: SimulationEngine.java**
- Vị trí: [EcosystemSimulation/src/ecosystem/logic/SimulationEngine.java](EcosystemSimulation/src/ecosystem/logic/SimulationEngine.java)
- Chức năng: quản lý `Grid`, cập nhật step/day, tick() để chạy một bước mô phỏng.
- Gọi `grid.stepAll()` mỗi tick, tăng biến `step`, tính `day`, và (khi verbose) in ra dạng ASCII.
- Cung cấp `counts()` để UI hiển thị số lượng sinh vật.

---

Model core

**File: Organism.java**
- Vị trí: [EcosystemSimulation/src/ecosystem/models/Organism.java](EcosystemSimulation/src/ecosystem/models/Organism.java)
- Lớp trừu tượng cơ sở cho mọi sinh vật: giữ `id`, `x,y`, `energy`, `age`.
- `NEXT_ID` để cấp id nguyên tử; `getLabelId()` tạo nhãn kiểu `P00000001`.
- `step(Grid grid)` là abstract — mỗi loại định nghĩa hành vi.
- `isAlive()` kiểm tra `energy > 0`.

**File: OrganismSnapshot.java**
- Vị trí: [EcosystemSimulation/src/ecosystem/models/OrganismSnapshot.java](EcosystemSimulation/src/ecosystem/models/OrganismSnapshot.java)
- DTO bất biến dùng để hiển thị trong UI (ListView, panel). Có `from(Organism)` để tạo snapshot.

**File: Plant.java**
- Vị trí: [EcosystemSimulation/src/ecosystem/models/Plant.java](EcosystemSimulation/src/ecosystem/models/Plant.java)
- Hành vi: mỗi step có xác suất theo `grid.getPlantGrowRate()` để sinh mới tại ô lân cận trống; tăng `age`.
- `toString()` trả `'P'` cho hiển thị ASCII.

**File: Animal.java**
- Vị trí: [EcosystemSimulation/src/ecosystem/models/Animal.java](EcosystemSimulation/src/ecosystem/models/Animal.java)
- Lớp trừu tượng mở rộng `Organism` chứa `moveCost` và các phương thức di chuyển:
  - `moveTowards(targetX,targetY,grid)`: di chuyển 1 ô theo hướng mục tiêu nếu ô trống, trừ năng lượng.
  - `randomMove(grid)`: chọn ngẫu nhiên ô lân cận trống và di chuyển.

**File: Herbivore.java**
- Vị trí: [EcosystemSimulation/src/ecosystem/models/Herbivore.java](EcosystemSimulation/src/ecosystem/models/Herbivore.java)
- Hành vi `step`: tăng `age`; tìm cây lân cận (`Plant`) và ăn (xóa `Plant`, cộng năng lượng), nếu không thì di chuyển ngẫu nhiên; nếu năng lượng >= ngưỡng sinh sản thì phân chia (chia năng lượng, sinh con ở ô trống lân cận).
- `toString()` trả `'h'` (ASCII).

**File: Carnivore.java**
- Vị trí: [EcosystemSimulation/src/ecosystem/models/Carnivore.java](EcosystemSimulation/src/ecosystem/models/Carnivore.java)
- Hành vi `step`: tìm `Herbivore` lân cận, di chuyển tới và nếu cùng ô thì ăn (xóa con mồi, cộng năng lượng); nếu không tìm thấy thì di chuyển ngẫu nhiên; cũng có sinh sản khi đủ năng lượng.
- `toString()` trả `'C'`.

**File: Grid.java**
- Vị trí: [EcosystemSimulation/src/ecosystem/models/Grid.java](EcosystemSimulation/src/ecosystem/models/Grid.java)
- Chứa danh sách `organisms`, map id->organism (`idIndex`), kích thước lưới, tham số plant energy/grow rate, ngưỡng sinh sản.
- Cung cấp API quan trọng:
  - `addOrganism`, `removeOrganism`, `getOrganismAt`, `organismsAt`, `organisms` (toàn bộ danh sách).
  - `getNeighborPositions`, `getEmptyNeighbors`, `findNeighborOfType`.
  - `stepAll()` : sao chép danh sách hiện tại, gọi `step` cho từng `Organism`, lọc các sinh vật còn sống, phát sự kiện đến `GridListener` (added/removed/updated), cập nhật `organisms` và `idIndex`.
  - `populateBasic(...)` : khởi tạo số lượng ban đầu (Plant/Herbivore/Carnivore) tại vị trí ngẫu nhiên.
  - `asciiGrid()` trả danh sách chuỗi cho in console.

---

UI

**File: JavaFXApp.java**
- Vị trí: [EcosystemSimulation/src/ecosystem/ui/JavaFXApp.java](EcosystemSimulation/src/ecosystem/ui/JavaFXApp.java)
- Là ứng dụng JavaFX chính: tạo `SimulationEngine` từ `Settings`, khởi khung giao diện gồm:
  - Menu chính (Start/Settings/About/Help/Exit).
  - Bảng điều khiển (Start/Pause/Step/Reset, slider tốc độ).
  - Canvas trung tâm để vẽ lưới (dùng hình ảnh nếu có trong `icons/`).
  - Pane bên phải hiển thị `ListView<OrganismSnapshot>` và panel thuộc tính chi tiết.
  - Overview mini-map ở bên trái (scaled rendering).
- Kết nối UI với model:
  - Đăng ký `engine.grid.addListener(...)` để cập nhật `orgListView` khi có thay đổi incremental.
  - `timeline` (JavaFX `Timeline`) gọi `engine.tick()` theo chu kỳ, cập nhật summary và vẽ lại canvas.
  - Xử lý click trên canvas: tính toạ độ ô, chọn tổ chức tại ô (qua `SelectionManager`).

**File: SelectionManager.java**
- Vị trí: [EcosystemSimulation/src/ecosystem/ui/SelectionManager.java](EcosystemSimulation/src/ecosystem/ui/SelectionManager.java)
- Quản lý selection hiện tại thông qua `ObjectProperty<Integer>` (id của Organism). Dùng binding giữa `ListView` và canvas.

---

**Luồng tương tác chính**
- UI (JavaFXApp) tạo `SimulationEngine` → `SimulationEngine` chứa `Grid` → `Grid` chứa `Organism`.
- Mỗi tick: `SimulationEngine.tick()` gọi `Grid.stepAll()` → mỗi `Organism.step(grid)` có thể di chuyển, ăn, sinh sản hoặc chết.
- `Grid` thông báo listener (UI) về thêm/bớt/cập nhật, UI cập nhật `ListView` và panel.

**Gợi ý mở rộng / lưu ý**
- `Organism.getLabelId()` dùng để tạo mã người-dùng dễ đọc; id là incremental global.
- `Grid.GridListener` cung cấp hook để UI cập nhật incremental (hiện dùng Platform.runLater để cập nhật giao diện).
- Phần Settings dialog khởi tạo lại `SimulationEngine(settings)` khi Apply.
- Các tham số (moveCost, eatGain, thresholds) nằm trong `Settings` và được truyền khi populate.

**Cách chạy nhanh (local)**
1. Mở project và chạy `Main.main()` hoặc chạy `EcosystemSimulation` như ứng dụng JavaFX.
2. Hoặc biên dịch bằng công cụ IDE/`javac` và chạy `java ecosystem.Main` (đảm bảo classpath và JavaFX runtime).

---

Nếu bạn muốn, tôi có thể:
- Thêm chú thích dòng trong từng file (inline) hoặc tạo phiên bản Markdown tách từng file với trích đoạn mã.
- Tạo README.md làm hướng dẫn chạy và phát triển.

---

Tạo bởi GitHub Copilot — tóm tắt mã nguồn để dễ quản lý.
