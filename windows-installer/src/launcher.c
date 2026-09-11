typedef void *HANDLE;
typedef void *HINSTANCE;
typedef void *HWND;
typedef unsigned long DWORD;
typedef int BOOL;
typedef unsigned short WCHAR;
typedef const WCHAR *LPCWSTR;

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

#define MAX_PATH_LONG 32768
#define MAX_VERSION_PARTS 8
#define INVALID_FILE_ATTRIBUTES ((DWORD)-1)
#define INVALID_HANDLE_VALUE ((HANDLE)(long long)-1)
#define SW_SHOWNORMAL 1
#define MB_OK 0
#define MB_ICONERROR 0x10

__declspec(dllimport) DWORD __stdcall GetModuleFileNameW(HINSTANCE module, WCHAR *filename, DWORD size);
__declspec(dllimport) DWORD __stdcall GetFileAttributesW(LPCWSTR filename);
__declspec(dllimport) HANDLE __stdcall FindFirstFileW(LPCWSTR pattern, WIN32_FIND_DATAW *data);
__declspec(dllimport) BOOL __stdcall FindNextFileW(HANDLE handle, WIN32_FIND_DATAW *data);
__declspec(dllimport) BOOL __stdcall FindClose(HANDLE handle);
__declspec(dllimport) HINSTANCE __stdcall ShellExecuteW(HWND window, LPCWSTR operation,
        LPCWSTR file, LPCWSTR parameters, LPCWSTR directory, int showCommand);
__declspec(dllimport) int __stdcall MessageBoxW(HWND window, LPCWSTR text, LPCWSTR caption, unsigned int type);
__declspec(dllimport) long __stdcall SetCurrentProcessExplicitAppUserModelID(LPCWSTR appId);

static const WCHAR JAR_PREFIX[] = L"Cobblemon-Legacy-Launcher-";
static const WCHAR JAR_SUFFIX[] = L".jar";

static DWORD length_of(const WCHAR *value) {
    DWORD length = 0;
    while (value[length] != 0) length++;
    return length;
}

static BOOL append(WCHAR *target, DWORD capacity, const WCHAR *value) {
    DWORD targetLength = length_of(target);
    DWORD valueLength = length_of(value);
    if (targetLength + valueLength + 1 >= capacity) return 0;
    for (DWORD index = 0; index <= valueLength; index++) {
        target[targetLength + index] = value[index];
    }
    return 1;
}

static BOOL copy_text(WCHAR *target, DWORD capacity, const WCHAR *value) {
    target[0] = 0;
    return append(target, capacity, value);
}

static BOOL installation_directory(WCHAR *output, DWORD capacity) {
    DWORD length = GetModuleFileNameW(0, output, capacity);
    if (length == 0 || length >= capacity) return 0;
    while (length > 0) {
        length--;
        if (output[length] == '\\' || output[length] == '/') {
            output[length] = 0;
            return 1;
        }
    }
    return 0;
}

static BOOL same_prefix(const WCHAR *value, const WCHAR *prefix) {
    DWORD index = 0;
    while (prefix[index] != 0) {
        if (value[index] != prefix[index]) return 0;
        index++;
    }
    return 1;
}

static BOOL same_suffix(const WCHAR *value, DWORD valueLength, const WCHAR *suffix) {
    DWORD suffixLength = length_of(suffix);
    DWORD index;
    if (valueLength < suffixLength) return 0;
    for (index = 0; index < suffixLength; index++) {
        if (value[valueLength - suffixLength + index] != suffix[index]) return 0;
    }
    return 1;
}

static BOOL parse_version(const WCHAR *filename, DWORD parts[MAX_VERSION_PARTS]) {
    DWORD prefixLength = length_of(JAR_PREFIX);
    DWORD filenameLength = length_of(filename);
    DWORD suffixLength = length_of(JAR_SUFFIX);
    DWORD end;
    DWORD position;
    DWORD part = 0;
    BOOL hasDigit = 0;

    for (DWORD index = 0; index < MAX_VERSION_PARTS; index++) parts[index] = 0;
    if (!same_prefix(filename, JAR_PREFIX) || !same_suffix(filename, filenameLength, JAR_SUFFIX)) return 0;
    if (filenameLength <= prefixLength + suffixLength) return 0;

    end = filenameLength - suffixLength;
    for (position = prefixLength; position < end; position++) {
        WCHAR character = filename[position];
        if (character >= '0' && character <= '9') {
            hasDigit = 1;
            if (parts[part] < 100000000) parts[part] = parts[part] * 10 + (character - '0');
        } else if (character == '.' && hasDigit && part + 1 < MAX_VERSION_PARTS) {
            part++;
            hasDigit = 0;
        } else {
            return 0;
        }
    }
    return hasDigit;
}

static int compare_version(const DWORD left[MAX_VERSION_PARTS], const DWORD right[MAX_VERSION_PARTS]) {
    for (DWORD index = 0; index < MAX_VERSION_PARTS; index++) {
        if (left[index] > right[index]) return 1;
        if (left[index] < right[index]) return -1;
    }
    return 0;
}

static BOOL newest_launcher_jar(const WCHAR *directory, WCHAR *output, DWORD capacity) {
    WCHAR pattern[MAX_PATH_LONG] = {0};
    WCHAR selected[260] = {0};
    DWORD selectedVersion[MAX_VERSION_PARTS] = {0};
    WIN32_FIND_DATAW data;
    HANDLE search;
    BOOL found = 0;

    if (!append(pattern, MAX_PATH_LONG, directory)
            || !append(pattern, MAX_PATH_LONG, L"\\Cobblemon-Legacy-Launcher-*.jar")) return 0;
    search = FindFirstFileW(pattern, &data);
    if (search == INVALID_HANDLE_VALUE) return 0;

    do {
        DWORD candidateVersion[MAX_VERSION_PARTS];
        if (parse_version(data.fileName, candidateVersion)
                && (!found || compare_version(candidateVersion, selectedVersion) > 0)) {
            if (!copy_text(selected, 260, data.fileName)) continue;
            for (DWORD index = 0; index < MAX_VERSION_PARTS; index++) {
                selectedVersion[index] = candidateVersion[index];
            }
            found = 1;
        }
    } while (FindNextFileW(search, &data));
    FindClose(search);

    if (!found || !append(output, capacity, directory)
            || !append(output, capacity, L"\\")
            || !append(output, capacity, selected)) return 0;
    return GetFileAttributesW(output) != INVALID_FILE_ATTRIBUTES;
}

int __stdcall wWinMainCRTStartup(void) {
    static const WCHAR APP_ID[] = L"CobblemonLegacy.Launcher";
    static const WCHAR JAVA_SUFFIX[] = L"\\runtime\\bin\\javaw.exe";
    static const WCHAR PARAMETER_PREFIX[] = L"-Dfile.encoding=UTF-8 -jar \"";
    static const WCHAR QUOTE[] = L"\"";
    static const WCHAR OPEN[] = L"open";
    static const WCHAR TITLE[] = L"Cobblemon Legacy Launcher";
    static const WCHAR ERROR_TEXT[] = L"A instalacao esta incompleta ou nenhuma versao valida foi encontrada. Execute novamente o instalador conectado a internet.";

    WCHAR directory[MAX_PATH_LONG] = {0};
    WCHAR javaPath[MAX_PATH_LONG] = {0};
    WCHAR jarPath[MAX_PATH_LONG] = {0};
    WCHAR parameters[MAX_PATH_LONG] = {0};

    SetCurrentProcessExplicitAppUserModelID(APP_ID);
    if (!installation_directory(directory, MAX_PATH_LONG)
            || !append(javaPath, MAX_PATH_LONG, directory)
            || !append(javaPath, MAX_PATH_LONG, JAVA_SUFFIX)
            || !newest_launcher_jar(directory, jarPath, MAX_PATH_LONG)
            || GetFileAttributesW(javaPath) == INVALID_FILE_ATTRIBUTES
            || !append(parameters, MAX_PATH_LONG, PARAMETER_PREFIX)
            || !append(parameters, MAX_PATH_LONG, jarPath)
            || !append(parameters, MAX_PATH_LONG, QUOTE)) {
        MessageBoxW(0, ERROR_TEXT, TITLE, MB_OK | MB_ICONERROR);
        return 1;
    }

    HINSTANCE result = ShellExecuteW(0, OPEN, javaPath, parameters, directory, SW_SHOWNORMAL);
    if ((long long)result <= 32) {
        MessageBoxW(0, ERROR_TEXT, TITLE, MB_OK | MB_ICONERROR);
        return 2;
    }
    return 0;
}
