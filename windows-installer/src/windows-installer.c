typedef void *HANDLE;
typedef void *HINSTANCE;
typedef void *HWND;
typedef void *HICON;
typedef void *HRSRC;
typedef void *HGLOBAL;
typedef void *HKEY;
typedef unsigned long DWORD;
typedef unsigned long ULONG;
typedef unsigned long long ULONG_PTR;
typedef unsigned long long HCRYPTPROV;
typedef unsigned long long HCRYPTHASH;
typedef unsigned short WCHAR;
typedef unsigned char BYTE;
typedef int BOOL;
typedef unsigned int UINT;
typedef long HRESULT;
typedef long LONG;
typedef long long LPARAM;
typedef unsigned long long WPARAM;
typedef long long LRESULT;
typedef long long INT_PTR;
typedef const WCHAR *LPCWSTR;
typedef WCHAR *LPWSTR;
typedef void *LPVOID;

typedef struct {
    DWORD cb;
    LPWSTR reserved;
    LPWSTR desktop;
    LPWSTR title;
    DWORD x;
    DWORD y;
    DWORD xSize;
    DWORD ySize;
    DWORD xCountChars;
    DWORD yCountChars;
    DWORD fillAttribute;
    DWORD flags;
    unsigned short showWindow;
    unsigned short reserved2;
    BYTE *reserved2Data;
    HANDLE stdInput;
    HANDLE stdOutput;
    HANDLE stdError;
} STARTUPINFOW;

typedef struct {
    HANDLE process;
    HANDLE thread;
    DWORD processId;
    DWORD threadId;
} PROCESS_INFORMATION;

typedef struct {
    DWORD size;
    DWORD icc;
} INITCOMMONCONTROLSEX;

typedef struct {
    DWORD low;
    DWORD high;
} FILETIME;

typedef struct {
    DWORD attributes;
    FILETIME creationTime;
    FILETIME lastAccessTime;
    FILETIME lastWriteTime;
    DWORD fileSizeHigh;
    DWORD fileSizeLow;
    DWORD reserved0;
    DWORD reserved1;
    WCHAR fileName[260];
    WCHAR alternateFileName[14];
} WIN32_FIND_DATAW;

typedef struct {
    DWORD data1;
    unsigned short data2;
    unsigned short data3;
    BYTE data4[8];
} GUID;

typedef struct IShellLinkW IShellLinkW;
typedef struct IPersistFile IPersistFile;

typedef struct {
    HRESULT (__stdcall *QueryInterface)(IShellLinkW *, const GUID *, void **);
    ULONG (__stdcall *AddRef)(IShellLinkW *);
    ULONG (__stdcall *Release)(IShellLinkW *);
    void *GetPath;
    void *GetIDList;
    void *SetIDList;
    void *GetDescription;
    HRESULT (__stdcall *SetDescription)(IShellLinkW *, LPCWSTR);
    void *GetWorkingDirectory;
    HRESULT (__stdcall *SetWorkingDirectory)(IShellLinkW *, LPCWSTR);
    void *GetArguments;
    HRESULT (__stdcall *SetArguments)(IShellLinkW *, LPCWSTR);
    void *GetHotkey;
    void *SetHotkey;
    void *GetShowCmd;
    HRESULT (__stdcall *SetShowCmd)(IShellLinkW *, int);
    void *GetIconLocation;
    HRESULT (__stdcall *SetIconLocation)(IShellLinkW *, LPCWSTR, int);
    void *SetRelativePath;
    void *Resolve;
    HRESULT (__stdcall *SetPath)(IShellLinkW *, LPCWSTR);
} IShellLinkWVtbl;

struct IShellLinkW { IShellLinkWVtbl *lpVtbl; };

typedef struct {
    HRESULT (__stdcall *QueryInterface)(IPersistFile *, const GUID *, void **);
    ULONG (__stdcall *AddRef)(IPersistFile *);
    ULONG (__stdcall *Release)(IPersistFile *);
    void *GetClassID;
    void *IsDirty;
    void *Load;
    HRESULT (__stdcall *Save)(IPersistFile *, LPCWSTR, BOOL);
    void *SaveCompleted;
    void *GetCurFile;
} IPersistFileVtbl;

struct IPersistFile { IPersistFileVtbl *lpVtbl; };

#define MAX_PATH_LONG 32768
#define INVALID_HANDLE_VALUE ((HANDLE)(long long)-1)
#define INVALID_FILE_ATTRIBUTES ((DWORD)-1)
#define GENERIC_READ 0x80000000UL
#define GENERIC_WRITE 0x40000000UL
#define FILE_SHARE_READ 1
#define OPEN_EXISTING 3
#define CREATE_ALWAYS 2
#define OPEN_ALWAYS 4
#define FILE_ATTRIBUTE_NORMAL 0x80
#define FILE_END 2
#define MOVEFILE_REPLACE_EXISTING 1
#define MOVEFILE_WRITE_THROUGH 8
#define CREATE_NO_WINDOW 0x08000000
#define STARTF_USESHOWWINDOW 1
#define SW_HIDE 0
#define SW_SHOWNORMAL 1
#define WAIT_OBJECT_0 0
#define INFINITE 0xffffffffUL
#define WM_INITDIALOG 0x0110
#define WM_COMMAND 0x0111
#define WM_CLOSE 0x0010
#define WM_SETICON 0x0080
#define WM_USER 0x0400
#define WM_APP 0x8000
#define ICON_SMALL 0
#define ICON_BIG 1
#define PBM_SETMARQUEE (WM_USER + 10)
#define MB_OK 0
#define MB_ICONINFORMATION 0x40
#define MB_ICONERROR 0x10
#define MB_ICONWARNING 0x30
#define IDYES 6
#define IDD_INSTALLER 101
#define IDI_APP 1
#define IDR_RUNTIME 201
#define IDR_BOOTSTRAP 202
#define IDR_ICON_FILE 203
#define IDR_FALLBACK_JAR 204
#define IDC_STATUS 1001
#define IDC_PROGRESS 1002
#define IDC_INSTALL 1003
#define IDC_CANCEL 1004
#define ICC_PROGRESS_CLASS 0x20
#define RT_RCDATA 10
#define CSIDL_PROGRAMS 0x0002
#define CSIDL_DESKTOPDIRECTORY 0x0010
#define CSIDL_APPDATA 0x001a
#define CSIDL_LOCAL_APPDATA 0x001c
#define CSIDL_PROFILE 0x0028
#define CSIDL_FLAG_CREATE 0x8000
#define SHGFP_TYPE_CURRENT 0
#define COINIT_APARTMENTTHREADED 2
#define CLSCTX_INPROC_SERVER 1
#define PROV_RSA_AES 24
#define CRYPT_VERIFYCONTEXT 0xf0000000UL
#define CALG_SHA_256 0x0000800cUL
#define HP_HASHVAL 2

__declspec(dllimport) HINSTANCE __stdcall GetModuleHandleW(LPCWSTR);
__declspec(dllimport) DWORD __stdcall GetModuleFileNameW(HINSTANCE, LPWSTR, DWORD);
__declspec(dllimport) DWORD __stdcall GetLastError(void);
__declspec(dllimport) void __stdcall ExitProcess(UINT);
__declspec(dllimport) HANDLE __stdcall CreateThread(void *, ULONG_PTR, DWORD (__stdcall *)(void *), void *, DWORD, DWORD *);
__declspec(dllimport) BOOL __stdcall CloseHandle(HANDLE);
__declspec(dllimport) HRSRC __stdcall FindResourceW(HINSTANCE, LPCWSTR, LPCWSTR);
__declspec(dllimport) HGLOBAL __stdcall LoadResource(HINSTANCE, HRSRC);
__declspec(dllimport) DWORD __stdcall SizeofResource(HINSTANCE, HRSRC);
__declspec(dllimport) void *__stdcall LockResource(HGLOBAL);
__declspec(dllimport) HANDLE __stdcall CreateFileW(LPCWSTR, DWORD, DWORD, void *, DWORD, DWORD, HANDLE);
__declspec(dllimport) BOOL __stdcall WriteFile(HANDLE, const void *, DWORD, DWORD *, void *);
__declspec(dllimport) BOOL __stdcall ReadFile(HANDLE, void *, DWORD, DWORD *, void *);
__declspec(dllimport) DWORD __stdcall SetFilePointer(HANDLE, LONG, LONG *, DWORD);
__declspec(dllimport) BOOL __stdcall CreateDirectoryW(LPCWSTR, void *);
__declspec(dllimport) DWORD __stdcall GetFileAttributesW(LPCWSTR);
__declspec(dllimport) BOOL __stdcall DeleteFileW(LPCWSTR);
__declspec(dllimport) BOOL __stdcall MoveFileExW(LPCWSTR, LPCWSTR, DWORD);
__declspec(dllimport) DWORD __stdcall GetEnvironmentVariableW(LPCWSTR, LPWSTR, DWORD);
__declspec(dllimport) BOOL __stdcall CreateProcessW(LPCWSTR, LPWSTR, void *, void *, BOOL, DWORD, void *, LPCWSTR, STARTUPINFOW *, PROCESS_INFORMATION *);
__declspec(dllimport) DWORD __stdcall WaitForSingleObject(HANDLE, DWORD);
__declspec(dllimport) BOOL __stdcall GetExitCodeProcess(HANDLE, DWORD *);
__declspec(dllimport) HANDLE __stdcall FindFirstFileW(LPCWSTR, WIN32_FIND_DATAW *);
__declspec(dllimport) BOOL __stdcall FindNextFileW(HANDLE, WIN32_FIND_DATAW *);
__declspec(dllimport) BOOL __stdcall FindClose(HANDLE);

__declspec(dllimport) INT_PTR __stdcall DialogBoxParamW(HINSTANCE, LPCWSTR, HWND, INT_PTR (__stdcall *)(HWND, UINT, WPARAM, LPARAM), LPARAM);
__declspec(dllimport) HWND __stdcall GetDlgItem(HWND, int);
__declspec(dllimport) BOOL __stdcall SetWindowTextW(HWND, LPCWSTR);
__declspec(dllimport) BOOL __stdcall EnableWindow(HWND, BOOL);
__declspec(dllimport) LRESULT __stdcall SendMessageW(HWND, UINT, WPARAM, LPARAM);
__declspec(dllimport) BOOL __stdcall PostMessageW(HWND, UINT, WPARAM, LPARAM);
__declspec(dllimport) BOOL __stdcall EndDialog(HWND, INT_PTR);
__declspec(dllimport) int __stdcall MessageBoxW(HWND, LPCWSTR, LPCWSTR, UINT);
__declspec(dllimport) HICON __stdcall LoadIconW(HINSTANCE, LPCWSTR);
__declspec(dllimport) BOOL __stdcall UpdateWindow(HWND);
__declspec(dllimport) int __cdecl wsprintfW(LPWSTR, LPCWSTR, ...);

__declspec(dllimport) BOOL __stdcall InitCommonControlsEx(const INITCOMMONCONTROLSEX *);
__declspec(dllimport) HRESULT __stdcall SHGetFolderPathW(HWND, int, HANDLE, DWORD, LPWSTR);
__declspec(dllimport) HINSTANCE __stdcall ShellExecuteW(HWND, LPCWSTR, LPCWSTR, LPCWSTR, LPCWSTR, int);
__declspec(dllimport) HRESULT __stdcall CoInitializeEx(void *, DWORD);
__declspec(dllimport) void __stdcall CoUninitialize(void);
__declspec(dllimport) HRESULT __stdcall CoCreateInstance(const GUID *, void *, DWORD, const GUID *, void **);
__declspec(dllimport) HRESULT __stdcall URLDownloadToFileW(void *, LPCWSTR, LPCWSTR, DWORD, void *);
__declspec(dllimport) BOOL __stdcall CryptAcquireContextW(HCRYPTPROV *, LPCWSTR, LPCWSTR, DWORD, DWORD);
__declspec(dllimport) BOOL __stdcall CryptCreateHash(HCRYPTPROV, DWORD, ULONG_PTR, DWORD, HCRYPTHASH *);
__declspec(dllimport) BOOL __stdcall CryptHashData(HCRYPTHASH, const BYTE *, DWORD, DWORD);
__declspec(dllimport) BOOL __stdcall CryptGetHashParam(HCRYPTHASH, DWORD, BYTE *, DWORD *, DWORD);
__declspec(dllimport) BOOL __stdcall CryptDestroyHash(HCRYPTHASH);
__declspec(dllimport) BOOL __stdcall CryptReleaseContext(HCRYPTPROV, DWORD);

static const GUID CLSID_ShellLink = {0x00021401, 0x0000, 0x0000, {0xc0,0x00,0x00,0x00,0x00,0x00,0x00,0x46}};
static const GUID IID_IShellLinkW = {0x000214f9, 0x0000, 0x0000, {0xc0,0x00,0x00,0x00,0x00,0x00,0x00,0x46}};
static const GUID IID_IPersistFile = {0x0000010b, 0x0000, 0x0000, {0xc0,0x00,0x00,0x00,0x00,0x00,0x00,0x46}};
static const WCHAR TITLE[] = L"Cobblemon Legacy Launcher";
static const WCHAR LATEST_JAR_URL[] = L"https://github.com/velhotoby/launcher/releases/latest/download/Cobblemon-Legacy-Launcher-Windows.jar";
static const WCHAR LATEST_SHA_URL[] = L"https://github.com/velhotoby/launcher/releases/latest/download/Cobblemon-Legacy-Launcher-Windows.jar.sha256";

static HINSTANCE module;
static HWND dialogWindow;
static BOOL installing;
static WCHAR appDirectory[MAX_PATH_LONG];
static WCHAR logPath[MAX_PATH_LONG];
static WCHAR errorText[1024];

static DWORD length_of(const WCHAR *value) {
    DWORD length = 0;
    while (value && value[length] != 0) length++;
    return length;
}

static BOOL append(WCHAR *target, DWORD capacity, const WCHAR *value) {
    DWORD left = length_of(target);
    DWORD right = length_of(value);
    if (left + right + 1 > capacity) return 0;
    for (DWORD index = 0; index <= right; index++) target[left + index] = value[index];
    return 1;
}

static BOOL copy_text(WCHAR *target, DWORD capacity, const WCHAR *value) {
    target[0] = 0;
    return append(target, capacity, value);
}

static void set_error(LPCWSTR message) {
    copy_text(errorText, 1024, message);
}

static void set_last_error(LPCWSTR action) {
    wsprintfW(errorText, L"%s (erro do Windows %lu).", action, GetLastError());
}

static BOOL path_join(WCHAR *output, DWORD capacity, LPCWSTR base, LPCWSTR child) {
    if (!copy_text(output, capacity, base)) return 0;
    DWORD size = length_of(output);
    if (size > 0 && output[size - 1] != '\\' && !append(output, capacity, L"\\")) return 0;
    return append(output, capacity, child);
}

static BOOL ensure_directory(LPCWSTR directory) {
    WCHAR partial[MAX_PATH_LONG] = {0};
    if (!copy_text(partial, MAX_PATH_LONG, directory)) return 0;
    DWORD size = length_of(partial);
    for (DWORD index = 3; index <= size; index++) {
        WCHAR saved = partial[index];
        if (saved == '\\' || saved == 0) {
            partial[index] = 0;
            if (GetFileAttributesW(partial) == INVALID_FILE_ATTRIBUTES
                    && !CreateDirectoryW(partial, 0)
                    && GetFileAttributesW(partial) == INVALID_FILE_ATTRIBUTES) return 0;
            partial[index] = saved;
        }
    }
    return 1;
}

static void write_log(LPCWSTR message) {
    if (!logPath[0]) return;
    HANDLE file = CreateFileW(logPath, GENERIC_WRITE, FILE_SHARE_READ, 0, OPEN_ALWAYS,
            FILE_ATTRIBUTE_NORMAL, 0);
    if (file == INVALID_HANDLE_VALUE) return;
    SetFilePointer(file, 0, 0, FILE_END);
    DWORD written;
    const BYTE bomlessPrefix[] = {'[','W','I','N','D','O','W','S',']',' '};
    WriteFile(file, bomlessPrefix, sizeof(bomlessPrefix), &written, 0);
    DWORD count = length_of(message);
    for (DWORD index = 0; index < count; index++) {
        BYTE character = message[index] < 128 ? (BYTE)message[index] : (BYTE)'?';
        WriteFile(file, &character, 1, &written, 0);
    }
    const BYTE newline[] = {'\r','\n'};
    WriteFile(file, newline, 2, &written, 0);
    CloseHandle(file);
}

static void status(LPCWSTR message) {
    write_log(message);
    HWND control = GetDlgItem(dialogWindow, IDC_STATUS);
    if (control) {
        SetWindowTextW(control, message);
        UpdateWindow(control);
    }
}

static BOOL write_resource(UINT identifier, LPCWSTR destination) {
    HRSRC resource = FindResourceW(module, (LPCWSTR)(ULONG_PTR)identifier,
            (LPCWSTR)(ULONG_PTR)RT_RCDATA);
    if (!resource) return 0;
    HGLOBAL loaded = LoadResource(module, resource);
    void *data = loaded ? LockResource(loaded) : 0;
    DWORD size = SizeofResource(module, resource);
    if (!data || !size) return 0;
    HANDLE file = CreateFileW(destination, GENERIC_WRITE, 0, 0, CREATE_ALWAYS,
            FILE_ATTRIBUTE_NORMAL, 0);
    if (file == INVALID_HANDLE_VALUE) return 0;
    DWORD written = 0;
    BOOL okay = WriteFile(file, data, size, &written, 0) && written == size;
    CloseHandle(file);
    return okay;
}

static BOOL run_and_wait(LPCWSTR executable, WCHAR *commandLine, LPCWSTR workingDirectory) {
    STARTUPINFOW startup = {0};
    PROCESS_INFORMATION process = {0};
    startup.cb = sizeof(startup);
    startup.flags = STARTF_USESHOWWINDOW;
    startup.showWindow = SW_HIDE;
    if (!CreateProcessW(executable, commandLine, 0, 0, 0, CREATE_NO_WINDOW, 0,
            workingDirectory, &startup, &process)) return 0;
    DWORD result = WaitForSingleObject(process.process, INFINITE);
    DWORD exitCode = 1;
    if (result == WAIT_OBJECT_0) GetExitCodeProcess(process.process, &exitCode);
    CloseHandle(process.thread);
    CloseHandle(process.process);
    return result == WAIT_OBJECT_0 && exitCode == 0;
}

static BOOL extract_runtime(LPCWSTR archive) {
    WCHAR systemRoot[MAX_PATH_LONG] = {0};
    WCHAR tarPath[MAX_PATH_LONG] = {0};
    WCHAR command[MAX_PATH_LONG] = {0};
    if (!GetEnvironmentVariableW(L"SystemRoot", systemRoot, MAX_PATH_LONG)
            || !path_join(tarPath, MAX_PATH_LONG, systemRoot, L"System32\\tar.exe")
            || GetFileAttributesW(tarPath) == INVALID_FILE_ATTRIBUTES) {
        set_error(L"O extrator nativo do Windows (tar.exe) não foi encontrado. Windows 10 ou 11 atualizado é necessário.");
        return 0;
    }
    append(command, MAX_PATH_LONG, L"\"");
    append(command, MAX_PATH_LONG, tarPath);
    append(command, MAX_PATH_LONG, L"\" -xf \"");
    append(command, MAX_PATH_LONG, archive);
    append(command, MAX_PATH_LONG, L"\" -C \"");
    append(command, MAX_PATH_LONG, appDirectory);
    append(command, MAX_PATH_LONG, L"\"");
    if (!run_and_wait(tarPath, command, appDirectory)) {
        set_last_error(L"Não foi possível extrair o Java 21 usando o componente nativo do Windows");
        return 0;
    }
    return 1;
}

static BOOL sha256_file(LPCWSTR path, BYTE digest[32]) {
    HCRYPTPROV provider = 0;
    HCRYPTHASH hash = 0;
    HANDLE file = INVALID_HANDLE_VALUE;
    BOOL okay = 0;
    BYTE buffer[65536];
    DWORD read = 0;
    DWORD digestSize = 32;
    if (!CryptAcquireContextW(&provider, 0, 0, PROV_RSA_AES, CRYPT_VERIFYCONTEXT)) goto done;
    if (!CryptCreateHash(provider, CALG_SHA_256, 0, 0, &hash)) goto done;
    file = CreateFileW(path, GENERIC_READ, FILE_SHARE_READ, 0, OPEN_EXISTING,
            FILE_ATTRIBUTE_NORMAL, 0);
    if (file == INVALID_HANDLE_VALUE) goto done;
    while (ReadFile(file, buffer, sizeof(buffer), &read, 0) && read > 0) {
        if (!CryptHashData(hash, buffer, read, 0)) goto done;
    }
    if (read != 0) goto done;
    okay = CryptGetHashParam(hash, HP_HASHVAL, digest, &digestSize, 0) && digestSize == 32;
done:
    if (file != INVALID_HANDLE_VALUE) CloseHandle(file);
    if (hash) CryptDestroyHash(hash);
    if (provider) CryptReleaseContext(provider, 0);
    return okay;
}

static BYTE lower_hex(BYTE value) {
    if (value >= 'A' && value <= 'F') return value + ('a' - 'A');
    return value;
}

static BOOL verify_checksum(LPCWSTR filePath, LPCWSTR checksumPath) {
    HANDLE checksum = CreateFileW(checksumPath, GENERIC_READ, FILE_SHARE_READ, 0,
            OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, 0);
    if (checksum == INVALID_HANDLE_VALUE) return 0;
    BYTE expected[64];
    DWORD read = 0;
    BOOL okay = ReadFile(checksum, expected, 64, &read, 0) && read == 64;
    CloseHandle(checksum);
    if (!okay) return 0;
    BYTE digest[32];
    if (!sha256_file(filePath, digest)) return 0;
    static const BYTE digits[] = "0123456789abcdef";
    for (DWORD index = 0; index < 32; index++) {
        if (lower_hex(expected[index * 2]) != digits[digest[index] >> 4]
                || lower_hex(expected[index * 2 + 1]) != digits[digest[index] & 15]) return 0;
    }
    return 1;
}

static void delete_old_versioned_jars(void) {
    static const WCHAR versionedPrefix[] = L"Cobblemon-Legacy-Launcher-";
    WCHAR pattern[MAX_PATH_LONG] = {0};
    WIN32_FIND_DATAW data;
    if (!path_join(pattern, MAX_PATH_LONG, appDirectory, L"Cobblemon-Legacy-Launcher-*.jar")) return;
    HANDLE search = FindFirstFileW(pattern, &data);
    if (search == INVALID_HANDLE_VALUE) return;
    do {
        DWORD prefixLength = length_of(versionedPrefix);
        WCHAR firstVersionCharacter = data.fileName[prefixLength];
        if (firstVersionCharacter < '0' || firstVersionCharacter > '9') continue;
        WCHAR path[MAX_PATH_LONG] = {0};
        if (path_join(path, MAX_PATH_LONG, appDirectory, data.fileName)) DeleteFileW(path);
    } while (FindNextFileW(search, &data));
    FindClose(search);
}

static BOOL install_launcher_jar(LPCWSTR destination) {
    WCHAR download[MAX_PATH_LONG] = {0};
    WCHAR checksum[MAX_PATH_LONG] = {0};
    path_join(download, MAX_PATH_LONG, appDirectory, L"launcher-download.jar");
    path_join(checksum, MAX_PATH_LONG, appDirectory, L"launcher-download.sha256");
    DeleteFileW(download);
    DeleteFileW(checksum);
    status(L"Baixando a versão mais recente do GitHub...");
    HRESULT jarResult = URLDownloadToFileW(0, LATEST_JAR_URL, download, 0, 0);
    HRESULT shaResult = URLDownloadToFileW(0, LATEST_SHA_URL, checksum, 0, 0);
    BOOL remote = jarResult >= 0 && shaResult >= 0 && verify_checksum(download, checksum);
    DeleteFileW(checksum);
    if (remote) {
        if (!MoveFileExW(download, destination, MOVEFILE_REPLACE_EXISTING | MOVEFILE_WRITE_THROUGH)) {
            set_last_error(L"O download foi validado, mas não pôde ser instalado");
            return 0;
        }
        delete_old_versioned_jars();
        write_log(L"JAR mais recente baixado e validado por SHA-256.");
        return 1;
    }
    DeleteFileW(download);
    write_log(L"GitHub indisponível ou checksum inválido; usando a cópia segura incorporada.");
    return GetFileAttributesW(destination) != INVALID_FILE_ATTRIBUTES;
}

static BOOL create_shortcut(LPCWSTR shortcutPath, LPCWSTR javaw, LPCWSTR bootstrap, LPCWSTR icon) {
    IShellLinkW *link = 0;
    IPersistFile *persist = 0;
    WCHAR arguments[MAX_PATH_LONG] = {0};
    append(arguments, MAX_PATH_LONG, L"-Dfile.encoding=UTF-8 -jar \"");
    append(arguments, MAX_PATH_LONG, bootstrap);
    append(arguments, MAX_PATH_LONG, L"\"");
    HRESULT result = CoCreateInstance(&CLSID_ShellLink, 0, CLSCTX_INPROC_SERVER,
            &IID_IShellLinkW, (void **)&link);
    if (result < 0 || !link) {
        WCHAR diagnostic[128] = {0};
        wsprintfW(diagnostic, L"Falha CoCreateInstance do atalho: HRESULT %ld.", result);
        write_log(diagnostic);
        return 0;
    }
    result = link->lpVtbl->SetPath(link, javaw);
    if (result >= 0) result = link->lpVtbl->SetArguments(link, arguments);
    if (result >= 0) result = link->lpVtbl->SetWorkingDirectory(link, appDirectory);
    if (result >= 0) result = link->lpVtbl->SetDescription(link, L"Iniciar Cobblemon Legacy Launcher");
    if (result >= 0) result = link->lpVtbl->SetIconLocation(link, icon, 0);
    if (result >= 0) result = link->lpVtbl->SetShowCmd(link, SW_SHOWNORMAL);
    if (result >= 0) result = link->lpVtbl->QueryInterface(link, &IID_IPersistFile, (void **)&persist);
    if (result >= 0 && persist) result = persist->lpVtbl->Save(persist, shortcutPath, 1);
    if (persist) persist->lpVtbl->Release(persist);
    link->lpVtbl->Release(link);
    if (result < 0) {
        WCHAR diagnostic[128] = {0};
        wsprintfW(diagnostic, L"Falha ao salvar atalho: HRESULT %ld.", result);
        write_log(diagnostic);
    }
    return result >= 0;
}

static BOOL create_shortcuts(LPCWSTR javaw, LPCWSTR bootstrap, LPCWSTR icon) {
    WCHAR folder[MAX_PATH_LONG] = {0};
    WCHAR path[MAX_PATH_LONG] = {0};
    BOOL atLeastOne = 0;
    if (SHGetFolderPathW(0, CSIDL_DESKTOPDIRECTORY | CSIDL_FLAG_CREATE, 0,
            SHGFP_TYPE_CURRENT, folder) >= 0
            && path_join(path, MAX_PATH_LONG, folder, L"Cobblemon Legacy Launcher.lnk")) {
        atLeastOne |= create_shortcut(path, javaw, bootstrap, icon);
    }
    folder[0] = 0;
    path[0] = 0;
    if (SHGetFolderPathW(0, CSIDL_PROGRAMS | CSIDL_FLAG_CREATE, 0, SHGFP_TYPE_CURRENT, folder) >= 0
            && path_join(path, MAX_PATH_LONG, folder, L"Cobblemon Legacy")) {
        ensure_directory(path);
        WCHAR linkPath[MAX_PATH_LONG] = {0};
        if (path_join(linkPath, MAX_PATH_LONG, path, L"Cobblemon Legacy Launcher.lnk"))
            atLeastOne |= create_shortcut(linkPath, javaw, bootstrap, icon);
    }
    folder[0] = 0;
    path[0] = 0;
    if (SHGetFolderPathW(0, CSIDL_APPDATA | CSIDL_FLAG_CREATE, 0, SHGFP_TYPE_CURRENT, folder) >= 0
            && path_join(path, MAX_PATH_LONG, folder,
                    L"Microsoft\\Internet Explorer\\Quick Launch\\User Pinned\\TaskBar")) {
        ensure_directory(path);
        WCHAR linkPath[MAX_PATH_LONG] = {0};
        if (path_join(linkPath, MAX_PATH_LONG, path, L"Cobblemon Legacy Launcher.lnk"))
            create_shortcut(linkPath, javaw, bootstrap, icon);
    }
    return atLeastOne;
}

static BOOL configure_paths(void) {
    WCHAR localAppData[MAX_PATH_LONG] = {0};
    WCHAR profile[MAX_PATH_LONG] = {0};
    WCHAR logDirectory[MAX_PATH_LONG] = {0};
    if (SHGetFolderPathW(0, CSIDL_LOCAL_APPDATA | CSIDL_FLAG_CREATE, 0,
            SHGFP_TYPE_CURRENT, localAppData) < 0
            || !path_join(appDirectory, MAX_PATH_LONG, localAppData,
                    L"Programs\\Cobblemon Legacy Launcher")
            || !ensure_directory(appDirectory)) return 0;
    if (SHGetFolderPathW(0, CSIDL_PROFILE | CSIDL_FLAG_CREATE, 0,
            SHGFP_TYPE_CURRENT, profile) >= 0
            && path_join(logDirectory, MAX_PATH_LONG, profile,
                    L".cobblemon_legacy_launcher\\logs")
            && ensure_directory(logDirectory)) {
        path_join(logPath, MAX_PATH_LONG, logDirectory, L"windows-installer.log");
    }
    return 1;
}

static BOOL install_application(void) {
    WCHAR runtimeArchive[MAX_PATH_LONG] = {0};
    WCHAR javaw[MAX_PATH_LONG] = {0};
    WCHAR bootstrap[MAX_PATH_LONG] = {0};
    WCHAR icon[MAX_PATH_LONG] = {0};
    WCHAR launcherJar[MAX_PATH_LONG] = {0};
    status(L"Preparando a instalação na pasta do usuário...");
    if (!configure_paths()) {
        set_last_error(L"Não foi possível criar a pasta de instalação do usuário");
        return 0;
    }
    write_log(L"Instalador 2.0 iniciado sem usar executável auxiliar no diretório temporário.");
    path_join(runtimeArchive, MAX_PATH_LONG, appDirectory, L"runtime-bundle.zip");
    path_join(bootstrap, MAX_PATH_LONG, appDirectory, L"Cobblemon-Legacy-Bootstrap.jar");
    path_join(icon, MAX_PATH_LONG, appDirectory, L"cobblemon-legacy.ico");
    path_join(launcherJar, MAX_PATH_LONG, appDirectory, L"Cobblemon-Legacy-Launcher-Windows.jar");
    if (!write_resource(IDR_RUNTIME, runtimeArchive)
            || !write_resource(IDR_BOOTSTRAP, bootstrap)
            || !write_resource(IDR_ICON_FILE, icon)
            || !write_resource(IDR_FALLBACK_JAR, launcherJar)) {
        set_last_error(L"Não foi possível gravar os componentes incorporados");
        return 0;
    }
    status(L"Instalando o Java 21 diretamente na pasta do usuário...");
    if (!extract_runtime(runtimeArchive)) return 0;
    DeleteFileW(runtimeArchive);
    path_join(javaw, MAX_PATH_LONG, appDirectory, L"runtime\\bin\\javaw.exe");
    if (GetFileAttributesW(javaw) == INVALID_FILE_ATTRIBUTES) {
        set_error(L"O runtime Java 21 extraído está incompleto.");
        return 0;
    }
    if (!install_launcher_jar(launcherJar)) return 0;
    status(L"Criando atalhos seguros para o Java instalado...");
    if (!create_shortcuts(javaw, bootstrap, icon)) {
        write_log(L"Aviso: o Windows recusou a criação de alguns atalhos.");
    }
    status(L"Instalação concluída com sucesso.");
    write_log(L"Instalação concluída.");
    return 1;
}

static void launch_installed(void) {
    WCHAR javaw[MAX_PATH_LONG] = {0};
    WCHAR bootstrap[MAX_PATH_LONG] = {0};
    WCHAR parameters[MAX_PATH_LONG] = {0};
    path_join(javaw, MAX_PATH_LONG, appDirectory, L"runtime\\bin\\javaw.exe");
    path_join(bootstrap, MAX_PATH_LONG, appDirectory, L"Cobblemon-Legacy-Bootstrap.jar");
    append(parameters, MAX_PATH_LONG, L"-Dfile.encoding=UTF-8 -jar \"");
    append(parameters, MAX_PATH_LONG, bootstrap);
    append(parameters, MAX_PATH_LONG, L"\"");
    ShellExecuteW(dialogWindow, L"open", javaw, parameters, appDirectory, SW_SHOWNORMAL);
}

static DWORD __stdcall install_worker(void *unused) {
    (void)unused;
    CoInitializeEx(0, COINIT_APARTMENTTHREADED);
    BOOL okay = install_application();
    CoUninitialize();
    PostMessageW(dialogWindow, okay ? WM_APP + 1 : WM_APP + 2, 0, 0);
    return okay ? 0 : 1;
}

static INT_PTR __stdcall dialog_proc(HWND window, UINT message, WPARAM wParam, LPARAM lParam) {
    (void)lParam;
    if (message == WM_INITDIALOG) {
        dialogWindow = window;
        HICON icon = LoadIconW(module, (LPCWSTR)(ULONG_PTR)IDI_APP);
        SendMessageW(window, WM_SETICON, ICON_BIG, (LPARAM)icon);
        SendMessageW(window, WM_SETICON, ICON_SMALL, (LPARAM)icon);
        return 1;
    }
    if (message == WM_COMMAND) {
        UINT command = (UINT)(wParam & 0xffff);
        if (command == IDC_INSTALL && !installing) {
            installing = 1;
            EnableWindow(GetDlgItem(window, IDC_INSTALL), 0);
            EnableWindow(GetDlgItem(window, IDC_CANCEL), 0);
            SendMessageW(GetDlgItem(window, IDC_PROGRESS), PBM_SETMARQUEE, 1, 30);
            HANDLE thread = CreateThread(0, 0, install_worker, 0, 0, 0);
            if (thread) CloseHandle(thread);
            else PostMessageW(window, WM_APP + 2, 0, 0);
            return 1;
        }
        if (command == IDC_CANCEL && !installing) {
            EndDialog(window, 0);
            return 1;
        }
    }
    if (message == WM_APP + 1) {
        installing = 0;
        SendMessageW(GetDlgItem(window, IDC_PROGRESS), PBM_SETMARQUEE, 0, 0);
        MessageBoxW(window,
                L"Instalação concluída. O launcher será aberto agora.\n\n"
                L"O instalador não executou arquivos próprios dentro do diretório temporário do Windows.",
                TITLE, MB_OK | MB_ICONINFORMATION);
        launch_installed();
        EndDialog(window, 1);
        return 1;
    }
    if (message == WM_APP + 2) {
        installing = 0;
        SendMessageW(GetDlgItem(window, IDC_PROGRESS), PBM_SETMARQUEE, 0, 0);
        EnableWindow(GetDlgItem(window, IDC_INSTALL), 1);
        EnableWindow(GetDlgItem(window, IDC_CANCEL), 1);
        SetWindowTextW(GetDlgItem(window, IDC_STATUS), L"A instalação não foi concluída.");
        MessageBoxW(window, errorText[0] ? errorText
                : L"Falha inesperada. Consulte windows-installer.log nos logs do launcher.",
                TITLE, MB_OK | MB_ICONERROR);
        return 1;
    }
    if (message == WM_CLOSE) {
        if (installing) {
            MessageBoxW(window, L"A instalação está em andamento. Aguarde a conclusão.",
                    TITLE, MB_OK | MB_ICONWARNING);
        } else EndDialog(window, 0);
        return 1;
    }
    return 0;
}

void __stdcall wWinMainCRTStartup(void) {
    module = GetModuleHandleW(0);
    INITCOMMONCONTROLSEX controls = {sizeof(controls), ICC_PROGRESS_CLASS};
    InitCommonControlsEx(&controls);
    CoInitializeEx(0, COINIT_APARTMENTTHREADED);
    INT_PTR result = DialogBoxParamW(module, (LPCWSTR)(ULONG_PTR)IDD_INSTALLER, 0, dialog_proc, 0);
    CoUninitialize();
    ExitProcess(result < 0 ? 1 : 0);
}
