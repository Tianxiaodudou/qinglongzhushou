#!/bin/bash
cd /tmp/qinglong_dev
FILES=(
  "app/src/main/java/com/qinglong/app/data/api/ApiManager.kt"
  "app/src/main/java/com/qinglong/app/data/api/AuthInterceptor.kt"
  "app/src/main/java/com/qinglong/app/data/api/LiveLoggingInterceptor.kt"
  "app/src/main/java/com/qinglong/app/data/api/QingLongApi.kt"
  "app/src/main/java/com/qinglong/app/data/model/Models.kt"
  "app/src/main/java/com/qinglong/app/data/repository/Repositories.kt"
  "app/src/main/java/com/qinglong/app/di/NetworkModule.kt"
  "app/src/main/java/com/qinglong/app/ui/QingLongNavHost.kt"
  "app/src/main/java/com/qinglong/app/ui/components/CommonComponents.kt"
  "app/src/main/java/com/qinglong/app/ui/screens/login/LoginScreen.kt"
  "app/src/main/java/com/qinglong/app/ui/screens/login/LoginViewModel.kt"
  "app/src/main/java/com/qinglong/app/ui/screens/task/TaskScreen.kt"
  "app/src/main/java/com/qinglong/app/ui/screens/task/TaskViewModel.kt"
  "app/src/main/java/com/qinglong/app/ui/theme/Color.kt"
  "app/src/main/java/com/qinglong/app/util/LiveLogger.kt"
)
for f in "${FILES[@]}"; do
  git show c632a2d:"$f" > "$f" 2>/dev/null && echo "OK: $f" || echo "FAIL: $f"
done
