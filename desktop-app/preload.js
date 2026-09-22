const { contextBridge, ipcRenderer } = require('electron');

// Secure contextBridge exposing window.desktopBridge
contextBridge.exposeInMainWorld('desktopBridge', {
    isElectron: true,
    platform: process.platform,

    // Native OS Notifications
    showNotification: (options) => ipcRenderer.invoke('show-native-notification', options),

    // Native File Save Dialog with UTF-8 BOM CSV support
    saveCsvFile: (options) => ipcRenderer.invoke('save-file-dialog', options),

    // Window controls
    minimize: () => ipcRenderer.send('window-minimize'),
    maximize: () => ipcRenderer.send('window-maximize'),
    close: () => ipcRenderer.send('window-close'),

    // System Information
    getSystemInfo: () => ipcRenderer.invoke('get-system-info'),

    // Main Process IPC Event Listeners
    onNavigateTab: (callback) => {
        ipcRenderer.on('navigate-tab', (event, tabName) => callback(tabName));
    },
    onTriggerExport: (callback) => {
        ipcRenderer.on('trigger-export', (event, exportType) => callback(exportType));
    }
});
