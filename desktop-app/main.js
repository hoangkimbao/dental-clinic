const { app, BrowserWindow, ipcMain, Notification, Tray, Menu, dialog, shell, nativeImage } = require('electron');
const path = require('path');
const fs = require('fs');

let mainWindow = null;
let tray = null;
let isQuitting = false;

const isDev = process.argv.includes('--dev') || process.env.NODE_ENV === 'development';

// 1. Single Instance Lock (Enterprise stability)
const gotTheLock = app.requestSingleInstanceLock();
if (!gotTheLock) {
    app.quit();
} else {
    app.on('second-instance', () => {
        if (mainWindow) {
            if (mainWindow.isMinimized()) mainWindow.restore();
            mainWindow.focus();
            mainWindow.show();
        }
    });
}

function createMainWindow() {
    mainWindow = new BrowserWindow({
        width: 1440,
        height: 920,
        minWidth: 1100,
        minHeight: 720,
        title: 'DentalCare Luxury Clinic — PC Command Center & Notification Hub',
        backgroundColor: '#0b1120',
        show: false, // Smooth show after ready-to-show
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            nodeIntegration: false,
            contextIsolation: true,
            sandbox: false, // Allows secure context bridge IPC with preload
            webSecurity: true,
            devTools: true
        }
    });

    mainWindow.loadFile(path.join(__dirname, 'index.html'));

    mainWindow.once('ready-to-show', () => {
        mainWindow.show();
        if (isDev) {
            mainWindow.webContents.openDevTools({ mode: 'detach' });
        }
    });

    // Handle minimize to tray instead of quitting if preferred
    mainWindow.on('close', (event) => {
        if (!isQuitting) {
            event.preventDefault();
            mainWindow.hide();
            if (Notification.isSupported()) {
                new Notification({
                    title: 'DentalCare Command Center',
                    body: 'Ứng dụng vẫn đang chạy ngầm trong khay hệ thống (System Tray) để nhận thông báo thời gian thực.'
                }).show();
            }
        }
        return false;
    });

    mainWindow.on('closed', () => {
        mainWindow = null;
    });
}

// 2. System Tray Management
function createTray() {
    // Generate a minimal 16x16 / 32x32 SVG or native image for tray
    const svgIcon = `
    <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="#0284c7">
        <path d="M12 2C7.58 2 4 5.58 4 10c0 3.32 2.01 6.17 4.88 7.35L8.5 21a1 1 0 0 0 1.5.86L12 20.73l1.99 1.13a1 1 0 0 0 1.5-.86l-.38-3.65C17.99 16.17 20 13.32 20 10c0-4.42-3.58-8-8-8zm0 2c3.31 0 6 2.69 6 6 0 2.45-1.48 4.56-3.6 5.48l-.4.18.47 4.54-1.47-.84a1 1 0 0 0-1 0l-1.47.84.47-4.54-.4-.18C8.48 14.56 7 12.45 7 10c0-3.31 2.69-6 5-6z"/>
    </svg>`;
    const trayIcon = nativeImage.createFromBuffer(Buffer.from(svgIcon));

    tray = new Tray(trayIcon);
    tray.setToolTip('DentalCare Clinic — Command Center');

    const contextMenu = Menu.buildFromTemplate([
        {
            label: 'Mở Command Center',
            click: () => {
                if (mainWindow) {
                    mainWindow.show();
                    mainWindow.focus();
                }
            }
        },
        {
            label: 'Trung Tâm Thông Báo (Live Alert)',
            click: () => {
                if (mainWindow) {
                    mainWindow.show();
                    mainWindow.webContents.send('navigate-tab', 'notifications');
                }
            }
        },
        { type: 'separator' },
        {
            label: 'Xuất Báo Cáo Nhanh (Excel/CSV)',
            submenu: [
                {
                    label: 'Xuất Lịch Hẹn Khám',
                    click: () => {
                        if (mainWindow) {
                            mainWindow.show();
                            mainWindow.webContents.send('trigger-export', 'appointments');
                        }
                    }
                },
                {
                    label: 'Xuất Tồn Kho Vật Tư',
                    click: () => {
                        if (mainWindow) {
                            mainWindow.show();
                            mainWindow.webContents.send('trigger-export', 'inventory');
                        }
                    }
                },
                {
                    label: 'Xuất KPI Bác Sĩ',
                    click: () => {
                        if (mainWindow) {
                            mainWindow.show();
                            mainWindow.webContents.send('trigger-export', 'kpi');
                        }
                    }
                },
                {
                    label: 'Xuất Tiếp Nhận Hiện Trường',
                    click: () => {
                        if (mainWindow) {
                            mainWindow.show();
                            mainWindow.webContents.send('trigger-export', 'intake');
                        }
                    }
                }
            ]
        },
        { type: 'separator' },
        {
            label: 'Thoát Hoàn Toàn',
            click: () => {
                isQuitting = true;
                app.quit();
            }
        }
    ]);

    tray.setContextMenu(contextMenu);
    tray.on('double-click', () => {
        if (mainWindow) {
            mainWindow.isVisible() ? mainWindow.hide() : mainWindow.show();
        }
    });
}

// 3. IPC Handlers: Native Notifications, File Dialogs, Window Controls

// Native OS Notifications
ipcMain.handle('show-native-notification', async (event, { title, body, silent = false }) => {
    if (Notification.isSupported()) {
        const notif = new Notification({
            title: title || 'DentalCare Clinic Alert',
            body: body || '',
            silent: silent
        });

        notif.on('click', () => {
            if (mainWindow) {
                if (mainWindow.isMinimized()) mainWindow.restore();
                mainWindow.show();
                mainWindow.focus();
                mainWindow.webContents.send('navigate-tab', 'notifications');
            }
        });

        notif.show();
        return { success: true };
    }
    return { success: false, reason: 'Notifications not supported on this OS' };
});

// Native File Save Dialog with UTF-8 BOM CSV support
ipcMain.handle('save-file-dialog', async (event, { defaultPath, data, encoding = 'utf-8' }) => {
    try {
        const { canceled, filePath } = await dialog.showSaveDialog(mainWindow, {
            title: 'Lưu Báo Cáo Excel/CSV Nha Khoa',
            defaultPath: defaultPath || 'bao-cao-dentalcare.csv',
            filters: [
                { name: 'CSV File (Hỗ trợ tiếng Việt Excel)', extensions: ['csv'] },
                { name: 'Tất cả tập tin', extensions: ['*'] }
            ]
        });

        if (canceled || !filePath) {
            return { canceled: true };
        }

        // Ensure UTF-8 BOM (\uFEFF) exists for Excel compatibility
        let bufferToWrite;
        if (typeof data === 'string') {
            const hasBom = data.startsWith('\uFEFF');
            const finalString = hasBom ? data : '\uFEFF' + data;
            bufferToWrite = Buffer.from(finalString, 'utf-8');
        } else if (Buffer.isBuffer(data)) {
            // Check if buffer starts with EF BB BF
            const hasBom = data.length >= 3 && data[0] === 0xEF && data[1] === 0xBB && data[2] === 0xBF;
            bufferToWrite = hasBom ? data : Buffer.concat([Buffer.from([0xEF, 0xBB, 0xBF]), data]);
        } else {
            bufferToWrite = Buffer.from(JSON.stringify(data), 'utf-8');
        }

        await fs.promises.writeFile(filePath, bufferToWrite);
        return { success: true, filePath };
    } catch (err) {
        console.error('Error in save-file-dialog:', err);
        return { success: false, error: err.message };
    }
});

// Window Controls
ipcMain.on('window-minimize', () => {
    if (mainWindow) mainWindow.minimize();
});

ipcMain.on('window-maximize', () => {
    if (mainWindow) {
        mainWindow.isMaximized() ? mainWindow.unmaximize() : mainWindow.maximize();
    }
});

ipcMain.on('window-close', () => {
    if (mainWindow) mainWindow.close();
});

// System Information
ipcMain.handle('get-system-info', async () => {
    return {
        platform: process.platform,
        arch: process.arch,
        electronVersion: process.versions.electron,
        chromeVersion: process.versions.chrome,
        nodeVersion: process.versions.node,
        appVersion: app.getVersion(),
        uptimeSeconds: Math.floor(process.uptime()),
        memoryUsageMb: Math.round(process.memoryUsage().heapUsed / 1024 / 1024)
    };
});

// App Lifecycle
app.whenReady().then(() => {
    createMainWindow();
    createTray();

    app.on('activate', () => {
        if (BrowserWindow.getAllWindows().length === 0) {
            createMainWindow();
        } else if (mainWindow) {
            mainWindow.show();
        }
    });
});

app.on('before-quit', () => {
    isQuitting = true;
});

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
        app.quit();
    }
});
