# Zuvo - social media app (Spring Boot + HTML/CSS/JS)

## 1. One-time setup
1. Install JDK 21, Eclipse IDE for Enterprise Java and Web Developers, MySQL Server + Workbench.
2. In MySQL Workbench open `sql/zuvo_schema.sql` and run it (creates database `zuvo_db` and all tables).
3. Open `src/main/resources/application-local.properties` and paste YOUR keys (MySQL password, Gmail app password,
   Cloudinary, OpenAI moderation key, LiveKit). This file is private and ignored by Git.
4. Eclipse: File > Import > Maven > Existing Maven Projects > choose this folder > Finish.
   (Wait for "Building workspace" and Maven downloads to finish.)

## 2. Run
- Open `src/main/java/com/zuvo/ZuvoApplication.java` > right-click > Run As > Java Application
  (or Spring Boot App if Spring Tools is installed).
- Open http://localhost:8080 in Chrome.
- The first start creates the "Zuvo Discover" account and sample posts automatically.
- While `zuvo.dev-log-codes=true`, the 5-digit email code is also printed in the Eclipse console.

## 3. Test on a phone (calls + live need HTTPS)
Run a tunnel such as `ngrok http 8080` and open the https link on your phone. Then use Chrome menu > Install app.

## Folder map
- sql/zuvo_schema.sql ............ MySQL database + tables
- src/main/java/com/zuvo ......... backend (entity, repo, service, web = REST controllers, ws = WebSocket, config)
- src/main/resources/static ...... frontend (index.html, css/style.css, js/*.js, manifest.json, sw.js, icons)
