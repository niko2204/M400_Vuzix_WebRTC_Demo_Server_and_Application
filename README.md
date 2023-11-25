# WebRTC 서버와 뷰직스 M400을 위한 앱 (smart glass) - 목포대학교 재난안전사업단

## 로컬 P2P 서버 설치 및 실행 방법

Nodejs 를 서버나 워크스테이션에 설치하세요. 
server 디렉터로로 이동하여 아래 명령어를 실행하시면 서버가 실행됩니다.

    npm install
    npm start

## Configuration paramerter

config.json 파일을 열어 아래와 같은 설정을 수정할 수 있다.

해당 구성을 사용하려면 서버를 시작하기 전에 구성을 수정하고 저장해야 합니다.

   p2p - 일반: 8095, - P2P 암호화되지 않은 포트
   p2p - 보안: 8096, - 피어 투 피어 TLS 포트

   웹 - 일반: 8080, - 웹 클라이언트 암호화되지 않은 포트
   웹 - 보안: 8433, - 웹 클라이언트 TLS 포트

   ip_address - 자동: true, - 자동 IP 감지 활성화,
   ip_address - 주소: "192.168.1.2", - 자동 IP 감지가 비활성화된 경우 사용되는 문자열 주소입니다.


## 로컬 P2P 및 웹 서버 중지

P2P와 웹 서버를 중지하려면 NPM 프로세스를 종료하면 됩니다.
포트 잠금과 관련된 알려진 문제가 있으므로 백그라운드에서 실행 중인 NodeJS 프로세스를 중지해야 합니다.

## 안드로이드 클라이언트 애플리케이션
Android 스튜디오 3.0 이상 설치
기존 안드로이드 프로젝트 열기
VuzixHUD 디렉토리를 선택하세요
usb 케이블을 이용하여 노트북과 M400 연결 (M400 개발자 모드 활성화 할 것)
Vuzix M400에 애플리케이션 빌드 및 배포

사전 컴파일된 APK는 ADB 명령줄 도구를 사용하여 설치할 수 있는 bin 디렉터리에 제공됩니다.

    adb install vuzix-xrclient-debug.apk

## M300, M400, Blade Button Interface

Application navigation: The "menu" gesture will bring up a menu. The "back" gesture will exit application.


## 사용법

1. 서버를 시작합니다.
2. 웹 포털을 통해 p2p 신호 서버에 로그인합니다(로그인 버튼 클릭).
3. M400/M4000의 카메라로 사용하여 웹 포털에 표시된 QR 코드를 스캔하여 p2p 서버에 로그인합니다.
4. 웹 포털을 통해 연결하려면 M400를 초대하세요(초대 버튼 클릭).
5. 이제 P2P 연결을 사용하여 서로 비디오/오디오/텍스트를 공유할 수 있습니다.
6. 안드로이드 앱의 overlay권한을 수동으로 허용합니다.
## User Interface

Main screen Video is shared video

Chat messages send from the Desktop client display briefly.

Known Issues: All devices must have unique names.
Work around: Use unique names for each device.


## Not Implemented:
Sharing desktop from PC to glasses requires plugin for Chrome and Signed Certificates.
Secured endpoints, all code exists for implementation.  Requires a signed certificate to secure the endpoints and changing the portal
	to use HTTPS and clients to use wss for websockets on port 8096.

Note: HTTPS required to access camera when not hosted locally

## update
* 2023.11.24
  시연 도중 터치패드 입력으로 인해 오작동이 발생많이 발생함.
  터치패드 입력과 버튼 입력이 같은 키 이벤트를 사용하여 둘 다 막을 수 밖에 없음.
  세손가락 터치만 남겨두고 모두 막음. 세손가락 길게 터치시 전원끄기 혹은 재부팅 가능.
  
## Original Projects:
* All project are deriviate work from Open WebRTC Toolkit
https://github.com/open-webrtc-toolkit

* Server: https://github.com/open-webrtc-toolkit/owt-server-p2p
App: https://github.com/open-webrtc-toolkit/owt-client-android

* M300_Vuzix_WebRTC_Demo_Server_and_Application
https://github.com/Vuzix/M300_Vuzix_WebRTC_Demo_Server_and_Application

* 시작시 부팅 방법, step을 이용한 intent
https://github.com/niko2204/walkNcamera
