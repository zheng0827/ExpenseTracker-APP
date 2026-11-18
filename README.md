# 簡記（OneLedger）· 三星原生 Android 記帳 App

專為三星 Galaxy（One UI）設計的原生 Android 離線優先記帳應用（Kotlin + Jetpack Compose + Room SQLite + Jetpack Glance 桌面小工具 + CameraX + WorkManager），並附帶輕量多裝置 SQLite 雲端同步伺服器。

## 核心架構

```
ExpenseTracker/
├── android/                        # 原生 Android App（Kotlin / Jetpack Compose）
│   └── app/src/main/java/com/oneledger/samsung/
│       ├── data/ledger/            # ledger.db：收支明細、循環規則、信用卡點數回饋、待確認重複
│       ├── data/debt/              # debts.db：完全獨立的借還款資料庫（絕不污染收支統計）
│       ├── data/SecurePrefs.kt     # Android Keystore AES256-GCM 加密儲存金鑰與 Token
│       ├── domain/                 # 本地卡號後四碼遮蔽、Gemini + 結構化本地解析、回饋計算器
│       ├── service/                # 原生 Android NotificationListenerService 與系統通知
│       ├── worker/                 # WorkManager：雲端同步、循環到期自動入帳、每日記帳提醒
│       ├── widget/                 # Jetpack Glance 原生桌面小工具（4×2 結餘快記、4×1 快記列）
│       └── ui/                     # 三星 One UI 深色 AMOLED / 淺色介面與二級設定子頁
├── sync-server/                    # 輕量自架多裝置 SQLite 同步伺服器（Node 內建 node:sqlite）
└── .github/workflows/              # GitHub Actions 雲端自動打包 APK（免本機 Android Studio）
```

## 主要功能

- **離線優先雙 SQLite 資料庫**：
  - `ledger.db`：收支紀錄（單一正規化 `description`）、循環規則、信用卡回饋規則、待確認重複。
  - `debts.db`：完全獨立的借還款資料庫，結清僅更新備忘狀態，不計入收入或支出。
- **右下角 `+` 單手手勢（無側邊返回衝突）**：
  - **短按**：標準手動新增（100% 滿寬收支條、Icon 分類格、固定 `308 dp` 等高小計算機／CameraX 下半屏拍照）。
  - **長按滑動（330° / 0° / 30°）**：語音記帳（震動＋Android `SpeechRecognizer` `zh-TW` → LLM）、文字記帳（1/3 高度 LLM 語句正規化）、拍照記帳（下半屏 CameraX 先拍後填）。
- **原生 Android 通知監聽服務（含本地卡號去敏）**：
  - 於「設定 → 通知監聽器與卡號去敏」開啟系統通知存取權後，即時攔截指定銀行／行動支付推播。
  - **本地先將信用卡末四碼、完整卡號、CVV 與 OTP 馬賽克遮蔽**，再交由 LLM 解析入帳並主動推播完成通知。
  - 同金額但商戶不確定時，移入首頁「疑似重複待確認」卡片，不再靜默誤刪同價消費。
- **信用卡點數回饋（不做帳戶餘額）**：
  - 可設定多張卡片的一般／指定分類加碼／海外回饋率，並支援勾選「海外 1.5% 國外交易手續費」試算淨回饋。
- **Jetpack Glance 桌面小工具與 WorkManager 背景排程**：
  - 4×2「收支結餘與快記」、4×1「今日支出快記列」直接釘選到三星 One UI 桌面。
  - 背景自動執行：循環訂閱到期入帳、每日「今天記帳了嗎？」提醒、有網路時增量同步多裝置。

---

## 免 Android Studio 取得 `.apk` 安裝到三星手機

1. 在 GitHub 建立一個私有（Private）Repository，將本資料夾推上 `main` 分支：
   ```bash
   git init
   git add .
   git commit -m "Initial native Android OneLedger app"
   git branch -M main
   git remote add origin https://github.com/<你的帳號>/<你的倉庫>.git
   git push -u origin main
   ```
2. 打開 GitHub 網頁 → **Actions** → 點選最新的 **Build Samsung OneLedger Android APK** 任務。
3. 等待約 2–3 分鐘編譯完成，於下方 **Artifacts** 下載 `OneLedger-Samsung-APK`（內含 `app-debug.apk`），傳到三星手機點擊安裝即可。

---

## 架設多裝置 SQLite 雲端同步伺服器（選用）

在任何一台具備 Node.js 22.5+（或 Node 25）的 VPS、NAS 或電腦執行：

```bash
cd sync-server
SYNC_TOKEN=換成你自己的私密金鑰 PORT=8787 npm start
```

接著在 App 的 **「設定 → 多裝置雲端 SQLite 同步」** 填入伺服器網址與同一組 `SYNC_TOKEN` 即可多裝置背景增量同步。
