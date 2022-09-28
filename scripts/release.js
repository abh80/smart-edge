const core = require("@actions/core");
const fs = require("fs");
const github = require("@actions/github");
const gradleBuild = fs.readFileSync(process.cwd() + "/app/build.gradle", {
  encoding: "ascii",
});
const version = gradleBuild
  .split("\n")
  .filter((x) => x.includes("versionName"))[0]
  .trim()
  .replace("versionName", "")
  .replace(/"/g, "")
  .toString();

(async () => {
  const TOKEN = process.env["TOKEN"];
  if (!TOKEN) return core.setFailed("No token was provided, exiting...");
  const octokit = github.getOctokit(TOKEN);
  const build_path = process.cwd() + "/app/app-release-unsigned.apk";
  const data = await octokit.rest.repos.createRelease({
    owner: "abh80",
    repo: "smart-edge",
    tag_name: version,
    name: version,
    body: "🎉 " + version + " Released!",
  });
  await octokit.rest.repos.uploadReleaseAsset({
    owner: "abh80",
    repo: "smart-edge",
    name: "release.apk",
    body: "Automatic Build",
    data: fs.readFileSync(build_path),
    release_id: data.data.id,
  });
})();
