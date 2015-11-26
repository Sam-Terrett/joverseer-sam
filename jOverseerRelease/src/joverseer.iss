[Setup]
AppCopyright=Marios Skounakis
AppName=JOverseer
AppVerName=JOverseer
DefaultDirName={pf}\JOverseer\
DefaultGroupName=JOverseer
OutputBaseFilename=joverseer-setup-@version@
#define InputResourceDirName "..\src"
#define InputBuild "..\dist"
; TODO: fix this so it doesn't need to know structure of joverseerjar
#define OrdercheckerMetadata "..\..\joverseerjar\resources\metadata\orderchecker"

DiskSpanning=false
DiskSliceSize=8000000

[Icons]
Name: "{group}\jOverseer"; Filename: "{app}\jOverseer.exe"; WorkingDir: "{app}"
Name: "{group}\Uninstall jOverseer"; Filename: "{uninstallexe}"

[Files]
Source: {#InputBuild}\..\MailSender\MailSender.exe; DestDir: {app}\bin\mailSender
Source: {#InputBuild}\default.layout; DestDir: {app}\layout
Source: {#InputBuild}\commons*.jar; DestDir: {app}
Source: {#InputBuild}\fo*.jar; DestDir: {app}
Source: {#InputBuild}\jide*.jar; DestDir: {app}
Source: {#InputBuild}\junit*.jar; DestDir: {app}
Source: {#InputBuild}\lo*.jar; DestDir: {app}
Source: {#InputBuild}\PDF*.jar; DestDir: {app}
Source: {#InputBuild}\skin*.jar; DestDir: {app}
Source: {#InputBuild}\spring*.jar; DestDir: {app}
Source: {#InputBuild}\swing*.jar; DestDir: {app}
Source: {#InputBuild}\txt2xml.jar; DestDir: {app}
Source: {#InputBuild}\update.jar; DestDir: {app}
Source: {#InputBuild}\OrderChecker.jar; DestDir: {app}
Source: {#InputBuild}\joverseer.jar; DestDir: {app}
Source: {#InputBuild}\joverseer.ico; DestDir: {app}
Source: {#InputBuild}\jOverseer.exe; DestDir: {app}
Source: {#InputBuild}\jOverseerUpdater.exe; DestDir: {app}
Source: {#InputResourceDirName}\joverseer.bat; DestDir: {app}
Source: {#InputResourceDirName}\jOverseer.lnk; DestDir: {userdesktop}
Source: {#InputBuild}\log4j.properties; DestDir: {app}
;check that these are actually needed here...they used to be copied in 1.0.12
Source: {#OrdercheckerMetadata}\*; DestDir: {app}\bin\metadata\orderchecker
;Source: {#InputBuild}\scope*.jar; DestDir: {app}
;Source: {#InputBuild}\jdom*.jar; DestDir: {app}

[Dirs]
Name: {app}\bin
Name: {app}\bin\mailSender
Name: {app}\bin\metadata
Name: {app}\bin\metadata\orderchecker
Name: {app}\layout
;check that these are actually needed here...they used to be copied in 1.0.12
Name: {app}\update
Name: {app}\meow


