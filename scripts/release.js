const core = require("@actions/core");
const fs = require("fs");
const github = require("@actions/github");
const gradleBuild = fs.readFileSync(process.cwd() + "/app/build.gradle", {
  encoding: "ascii",
});
const versionCode = gradleBuild
  .split("\n")
  .find((x) => x.includes("versionCode"))
  .trim()
  .replace("versionCode", "")
  .replace(/"/g, "")
  .trim();
const versionName = gradleBuild
  .split("\n")
  .find((x) => x.includes("versionName"))
  .trim()
  .replace("versionName", "")
  .replace(/"/g, "")
  .trim();
(async () => {
  const TOKEN = process.env["TOKEN"];
  if (!TOKEN) return core.setFailed("No token was provided, exiting...");
  const octokit = github.getOctokit(TOKEN);
  const build_path =
    process.cwd() + "/app/build/outputs/apk/release/app-release.apk";
  const data = await octokit.rest.repos.createRelease({
    owner: "abh80",
    repo: "smart-edge",
    tag_name: versionCode,
    name: "v" + versionName,
    body: "🎉 " + versionName + " Released!",
  });
  const fdata = await octokit.rest.repos.uploadReleaseAsset({
    owner: "abh80",
    repo: "smart-edge",
    name: "release.apk",
    body: "Automatic Build",
    data: fs.readFileSync(build_path),
    release_id: data.data.id,
  });
  const base_md = [
    `# Smart Edge (Early Access) [![Build & Publish Debug APK](https://github.com/abh80/smart-edge/actions/workflows/release.yml/badge.svg)](https://github.com/abh80/smart-edge/actions/workflows/release.yml)
  Alternative to dynamic island for android.`,
    `# Previews

  <img src = "https://user-images.githubusercontent.com/50198413/192252474-15852727-e487-4094-ae0f-bfc0f2c4ff06.png" width = "500"/>
  
  <img src = "https://user-images.githubusercontent.com/50198413/192252553-ee8fa52d-a3ec-4292-83a0-8ec3d9bb7787.png" width = "500"/>`,
  ];
  const downloadsmd = `# Downloads

  [![Download Button](https://img.shields.io/github/v/release/abh80/smart-edge?color=7885FF&label=Android-Apk&logo=android&style=for-the-badge)](${fdata.data.browser_download_url})`;
  fs.writeFileSync(
    process.cwd() + "/README.md",
    [base_md[0], downloadsmd, base_md[1]].join("\n")
  );
  core.setOutput("Successfully published apk!");
})();
