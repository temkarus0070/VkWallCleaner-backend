START /B D:\\soft\\keycloak-21.1.2\\bin\\kc.bat start-dev
cd C:\\Users\\temkarus0070\\WebstormProjects\\VkWallCleanerFrontend
START /B ng serve
timeout /t 50 /nobreak
START /B java -jar C:\\Users\\temkarus0070\\IdeaProjects\\VkWallCleaner\\target\\VkWallCleaner-0.0.1-SNAPSHOT.jar