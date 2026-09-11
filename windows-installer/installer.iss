#define AppName "Cobblemon Legacy Launcher"
#define InstallerVersion "1.0.0"
#define AppExeName "CobblemonLegacyLauncher.exe"

[Setup]
AppId={{58E469B9-A699-4B36-86FD-D454C3817D41}
AppName={#AppName}
AppVersion={#InstallerVersion}
AppVerName={#AppName}
AppPublisher=Comunidade Cobblemon Legacy
AppPublisherURL=https://cobblemonlegacy.com.br
AppSupportURL=https://cobblemonlegacy.com.br
AppUpdatesURL=https://github.com/velhotoby/launcher/releases/latest
DefaultDirName={localappdata}\Programs\Cobblemon Legacy Launcher
DefaultGroupName=Cobblemon Legacy
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
ArchitecturesAllowed=x64compatible
OutputDir=dist
OutputBaseFilename=Cobblemon-Legacy-Launcher-Installer
SetupIconFile=assets\cobblemon-legacy.ico
UninstallDisplayIcon={app}\{#AppExeName}
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
WizardSizePercent=110
WizardImageFile=assets\wizard-large.png
WizardSmallImageFile=assets\wizard-small.png
WizardImageStretch=yes
SetupLogging=yes
CloseApplications=yes
RestartApplications=no
VersionInfoVersion=1.0.0.0
VersionInfoCompany=Comunidade Cobblemon Legacy
VersionInfoDescription=Instalador online do Cobblemon Legacy Launcher
VersionInfoProductName=Cobblemon Legacy Launcher
VersionInfoProductVersion=1.0.0

[Languages]
Name: "brazilianportuguese"; MessagesFile: "compiler:Languages\BrazilianPortuguese.isl"

[Files]
Source: "payload\CobblemonLegacyLauncher.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "payload\runtime\*"; DestDir: "{app}\runtime"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "README-WINDOWS.txt"; DestDir: "{app}"; Flags: ignoreversion
Source: "{code:GetDownloadedJarPath}"; DestDir: "{app}"; DestName: "{code:GetLatestJarName}"; Flags: external ignoreversion

[Icons]
Name: "{group}\Cobblemon Legacy Launcher"; Filename: "{app}\{#AppExeName}"; WorkingDir: "{app}"; Comment: "Iniciar Cobblemon Legacy"
Name: "{group}\Desinstalar Cobblemon Legacy Launcher"; Filename: "{uninstallexe}"
Name: "{autodesktop}\Cobblemon Legacy Launcher"; Filename: "{app}\{#AppExeName}"; WorkingDir: "{app}"; Comment: "Iniciar Cobblemon Legacy"
Name: "{userappdata}\Microsoft\Internet Explorer\Quick Launch\User Pinned\TaskBar\Cobblemon Legacy Launcher"; Filename: "{app}\{#AppExeName}"; WorkingDir: "{app}"; Comment: "Iniciar Cobblemon Legacy"

[Run]
Filename: "{app}\{#AppExeName}"; Description: "Abrir o Cobblemon Legacy Launcher"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: files; Name: "{app}\Cobblemon-Legacy-Launcher-*.jar"
Type: dirifempty; Name: "{app}"

[Code]
const
  LatestApiUrl = 'https://api.github.com/repos/velhotoby/launcher/releases/latest';
  ReleaseBaseUrl = 'https://github.com/velhotoby/launcher/releases/download/';

var
  LatestVersion: String;
  LatestJarName: String;
  DownloadedJarPath: String;

function ExtractJsonString(const Json, Key: String): String;
var
  Marker: String;
  Tail: String;
  MarkerPosition: Integer;
  ColonPosition: Integer;
  StartPosition: Integer;
  EndPosition: Integer;
begin
  Result := '';
  Marker := '"' + Key + '"';
  MarkerPosition := Pos(Marker, Json);
  if MarkerPosition = 0 then Exit;

  Tail := Copy(Json, MarkerPosition + Length(Marker), MaxInt);
  ColonPosition := Pos(':', Tail);
  if ColonPosition = 0 then Exit;
  StartPosition := ColonPosition + 1;
  while (StartPosition <= Length(Tail)) and
        ((Tail[StartPosition] = ' ') or (Tail[StartPosition] = #9) or
         (Tail[StartPosition] = #10) or (Tail[StartPosition] = #13)) do
    StartPosition := StartPosition + 1;
  if (StartPosition > Length(Tail)) or (Tail[StartPosition] <> '"') then Exit;

  StartPosition := StartPosition + 1;
  EndPosition := StartPosition;
  while (EndPosition <= Length(Tail)) and (Tail[EndPosition] <> '"') do
    EndPosition := EndPosition + 1;
  if EndPosition > Length(Tail) then Exit;
  Result := Copy(Tail, StartPosition, EndPosition - StartPosition);
end;

function IsSafeVersion(const Value: String): Boolean;
var
  Index: Integer;
  HasDigit: Boolean;
begin
  Result := False;
  HasDigit := False;
  if (Length(Value) < 1) or (Length(Value) > 30) then Exit;
  for Index := 1 to Length(Value) do begin
    if (Value[Index] >= '0') and (Value[Index] <= '9') then
      HasDigit := True
    else if Value[Index] <> '.' then
      Exit;
  end;
  Result := HasDigit and (Value[1] <> '.') and (Value[Length(Value)] <> '.');
end;

function IsSafeSha256(const Value: String): Boolean;
var
  Index: Integer;
  Character: Char;
begin
  Result := False;
  if Length(Value) <> 64 then Exit;
  for Index := 1 to Length(Value) do begin
    Character := Value[Index];
    if not (((Character >= '0') and (Character <= '9')) or
            ((Character >= 'a') and (Character <= 'f')) or
            ((Character >= 'A') and (Character <= 'F'))) then Exit;
  end;
  Result := True;
end;

function OnDownloadProgress(const Url, FileName: String;
  const Progress, ProgressMax: Int64): Boolean;
begin
  if ProgressMax > 0 then
    WizardForm.ProgressGauge.Position := (Progress * 100) div ProgressMax;
  Result := True;
end;

procedure DownloadLatestLauncher;
var
  ApiPath: String;
  ApiJson: AnsiString;
  TagName: String;
  AssetSection: String;
  AssetPosition: Integer;
  Digest: String;
  DownloadUrl: String;
begin
  WizardForm.StatusLabel.Caption := 'Consultando a versão mais recente no GitHub...';
  ApiPath := ExpandConstant('{tmp}\cobblemon-latest-release.json');
  DownloadTemporaryFile(LatestApiUrl, 'cobblemon-latest-release.json', '', @OnDownloadProgress);
  if not LoadStringFromFile(ApiPath, ApiJson) then
    RaiseException('Não foi possível ler a resposta do GitHub.');

  TagName := ExtractJsonString(String(ApiJson), 'tag_name');
  LatestVersion := TagName;
  if (Length(LatestVersion) > 0) and
     ((LatestVersion[1] = 'v') or (LatestVersion[1] = 'V')) then
    Delete(LatestVersion, 1, 1);
  if not IsSafeVersion(LatestVersion) then
    RaiseException('O GitHub retornou uma versão inválida.');

  LatestJarName := 'Cobblemon-Legacy-Launcher-' + LatestVersion + '.jar';
  AssetPosition := Pos(LatestJarName, String(ApiJson));
  if AssetPosition = 0 then
    RaiseException('A release mais recente não contém o JAR oficial.');
  AssetSection := Copy(String(ApiJson), AssetPosition, MaxInt);
  Digest := ExtractJsonString(AssetSection, 'digest');
  if Pos('sha256:', Digest) <> 1 then
    RaiseException('A release não informou a assinatura SHA-256 do JAR.');
  Delete(Digest, 1, Length('sha256:'));
  if not IsSafeSha256(Digest) then
    RaiseException('A assinatura SHA-256 publicada é inválida.');

  DownloadUrl := ReleaseBaseUrl + TagName + '/' + LatestJarName;
  DownloadedJarPath := ExpandConstant('{tmp}\cobblemon-launcher-download.jar');
  WizardForm.StatusLabel.Caption := 'Baixando o Cobblemon Legacy Launcher ' + LatestVersion + '...';
  DownloadTemporaryFile(DownloadUrl, 'cobblemon-launcher-download.jar', Digest, @OnDownloadProgress);
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
begin
  Result := '';
  try
    DownloadLatestLauncher;
  except
    Result := 'Não foi possível baixar a versão mais recente do launcher.' + #13#10 +
      'Verifique sua conexão com a internet e tente novamente.' + #13#10#13#10 +
      GetExceptionMessage;
  end;
end;

function GetDownloadedJarPath(Param: String): String;
begin
  Result := DownloadedJarPath;
end;

function GetLatestJarName(Param: String): String;
begin
  Result := LatestJarName;
end;
