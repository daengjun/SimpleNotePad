# 📝 Simple Notepad  [![PlayStore](https://img.shields.io/badge/Download-Google%20Play-34A853?logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.simple.memo)  


**Simple Notepad**는 텍스트 기록에 집중한 간단한 메모 앱입니다.  
필요한 기능만 담아 누구나 쉽게 쓸 수 있도록 제작되었습니다.  

## 주요 기능

-  **홈 화면 위젯 지원**  
  원하는 메모를 선택해서 위젯에 고정할 수 있습니다.
-  **메모 자동 저장**  
  작성 중인 메모는 앱을 나가도 자동으로 저장됩니다.
-  **휴지통 기능**  
  실수로 삭제한 메모도 복구할 수 있어요
-  **텍스트 크기 조절**  
  작게 / 보통 / 크게로 편하게 설정할 수 있어요
-  **공유 기능**  
  메모 내용을 다른 앱으로 쉽게 공유할 수 있습니다.
-  **폴더별 분류**  
  메모를 원하는 폴더로 나눠 정리할 수 있어요 
  
## 📸 스크린샷  

| 전체 메모 | 글쓰기 화면 |
|--------|----------|
| ![Home](https://github.com/user-attachments/assets/5a0aada9-1022-4211-9fcd-b1cf2091a970) | ![Write](https://github.com/user-attachments/assets/e4e9585b-ddf9-4751-a944-8804e7700dc0) |

| 환경 설정 | 메모 위젯 |
|-----------|-----------|
| ![Setting](https://github.com/user-attachments/assets/f1a885c9-9c92-46c4-bafb-bd63e78b9789) | ![Widget](https://github.com/user-attachments/assets/76b16daf-b6c2-4329-8db0-8345e5e57059) |


## 📂 프로젝트 구조

com.simple.memo  
├── data         # Room 관련 (MemoEntity, MemoDao 등)  
├── ui           # UI 구성 (Fragment, Adapter, Activity 등)  
├── util         # 공통 유틸리티 (텍스트 사이즈 등)  
├── viewModel    # ViewModel 및 상태 관리   

## 🛠 사용된 기술 스택

- Kotlin
- MVVM + ViewModel
- Room Database
- DataBinding
- AppWidgetProvider (위젯)
- Material3 Components

## 🙋‍♂️ 개발자

- GitHub: [daengjun](https://github.com/daengjun)
- Email: jundroidx@gmail.com  

