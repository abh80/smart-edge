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
    let text = "🎉 v" + versionName + " Released!";
    const build_path =
        process.cwd() + "/app/build/outputs/apk/github/app-github.apk";
    const changelogFile = process.cwd() + "/fastlane/metadata/android/en-US/changelogs/" + versionCode + ".txt";
    if (fs.existsSync(changelogFile)) {
        text += "\n\n";
        text += "# Changelogs";
        text += "\n"
        text += fs.readFileSync(changelogFile, {encoding: "ascii"});
    }
    const data = await octokit.rest.repos.createRelease({
        owner: "abh80",
        repo: "smart-edge",
        tag_name: versionCode,
        name: "v" + versionName,
        body: text,
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
        `# Donations
  Help support the project by donating ❤️
  
  <a href="https://paypal.me/devabh80">
    <img width="300" src="https://raw.githubusercontent.com/stefan-niedermann/paypal-donate-button/master/paypal-donate-button.png" alt="Donate with PayPal" />
  </a>`,
    ];
    const downloadsmd = `# Downloads

  [![Download Button](https://img.shields.io/github/v/release/abh80/smart-edge?color=7885FF&label=Android-Apk&logo=android&style=for-the-badge)](${fdata.data.browser_download_url})`;
    const previewsMd = [`\n# Previews`];
    const screenshots = fs.readdirSync(
        process.cwd() + "/fastlane/metadata/android/en-US/images/phoneScreenshots"
    );
    screenshots.forEach((image) => {
        previewsMd.push(
            `<img src="./fastlane/metadata/android/en-US/images/phoneScreenshots/${image}" width="500"/>`
        );
    });
    fs.writeFileSync(
        process.cwd() + "/README.md",
        [
            base_md[0],
            downloadsmd,
            base_md[1],
            previewsMd.join("\n"),
        ].join("\n")
    );
    core.setOutput("Successfully published apk!");
})();
