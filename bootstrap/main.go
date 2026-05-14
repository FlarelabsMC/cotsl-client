package main

import (
	"archive/tar"
	"archive/zip"
	"bytes"
	"compress/gzip"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
)

var (
	appVersion = "devtest"
	repoOwner = "FlarelabsMC"
	repoName = "cotsl-client"
)


const javaVersion = "25"
const adoptiumURL = "https://api.adoptium.net/v3/binary/latest/%s/ga/%s/%s/jre/hotspot/normal/eclipse"

func jarDownloadURL() string {
	// e.g. https://github.com/FlarelabsMC/cotsl-client/releases/download/v1.0.0/cotsl-1.0.0-windows-x64.jar
	osTag := map[string]string{
		"windows": "windows-x64",
		"linux":   "linux-x64",
		"darwin":  "macos",
	}[runtime.GOOS]
	return fmt.Sprintf("https://github.com/%s/%s/releases/download/%s/cotsl-%s-%s.jar",
		repoOwner, repoName, appVersion, appVersion, osTag)
}

func main() {
    log.Print(appVersion)
	installDir := resolveInstallDir()
	os.MkdirAll(installDir, 0755)
	logFile, _ := os.OpenFile(filepath.Join(installDir, "bootstrap.log"), os.O_CREATE|os.O_APPEND|os.O_WRONLY|os.O_TRUNC, 0644)

	jarPath := filepath.Join(installDir, "cotsl.jar")
	versionPath := filepath.Join(installDir, "version.txt")
	runtimeDir := filepath.Join(installDir, "runtime", "jdk-"+javaVersion)
	javaExe := filepath.Join(runtimeDir, "bin", "java")
	if runtime.GOOS == "windows" {
		javaExe += "w.exe"
	}

	if needsJarUpdate(jarPath, versionPath) {
		if err := downloadJar(jarPath, versionPath, logFile); err != nil {
			fatalf(logFile, "Failed to download launcher: %v", err)
		}
	}

	if _, err := os.Stat(javaExe); os.IsNotExist(err) {
		if err := downloadJre(runtimeDir, logFile); err != nil {
			fatalf(logFile, "JRE download failed: %v", err)
		}
	}

	launchArgs := append([]string{
		"-javaagent:" + jarPath,
		"-jar", jarPath,
	}, os.Args[1:]...)

	cmd := exec.Command(javaExe, launchArgs...)
	cmd.Stdin = os.Stdin
	cmd.Stdout = logFile
	cmd.Stderr = logFile
	if err := cmd.Run(); err != nil {
		if exitErr, ok := err.(*exec.ExitError); ok {
			os.Exit(exitErr.ExitCode())
		}
		fatalf(logFile, "Launch failed: %v", err)
	}
}

func needsJarUpdate(jarPath, versionPath string) bool {
	if _, err := os.Stat(jarPath); os.IsNotExist(err) {
		return true
	}
	data, err := os.ReadFile(versionPath)
	if err != nil {
		return true
	}
	return strings.TrimSpace(string(data)) != appVersion
}

func downloadJar(jarPath, versionPath string, logFile *os.File) error {
	url := jarDownloadURL()
	logWrite(logFile, "[CotSL] Downloading launcher v"+appVersion+" from: "+url)

	resp, err := http.Get(url)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode != 200 {
		return fmt.Errorf("GitHub returned HTTP %d for JAR download", resp.StatusCode)
	}

	tmp, err := os.CreateTemp(filepath.Dir(jarPath), "cotsl-jar-*.tmp")
	if err != nil {
		return err
	}
	tmpName := tmp.Name()
	defer os.Remove(tmpName)

	if _, err := io.Copy(tmp, resp.Body); err != nil {
		tmp.Close()
		return err
	}
	tmp.Close()

	if err := os.Rename(tmpName, jarPath); err != nil {
		return err
	}
	return os.WriteFile(versionPath, []byte(appVersion), 0644)
}

func downloadJre(destDir string, logFile *os.File) error {
	osName := map[string]string{
		"windows": "windows",
		"linux":   "linux",
		"darwin":  "mac",
	}[runtime.GOOS]
	arch := "x64"
	if runtime.GOARCH == "arm64" {
		arch = "aarch64"
	}

	url := fmt.Sprintf(adoptiumURL, javaVersion, osName, arch)
	logWrite(logFile, "[CotSL] Downloading JRE from: "+url)

	resp, err := http.Get(url)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode != 200 {
		return fmt.Errorf("adoptium API returned HTTP %d", resp.StatusCode)
	}

	tmp, err := os.CreateTemp("", "cotsl-jre-*")
	if err != nil {
		return err
	}
	defer os.Remove(tmp.Name())
	if _, err := io.Copy(tmp, resp.Body); err != nil {
		return err
	}
	tmp.Close()

	if err := os.MkdirAll(destDir, 0755); err != nil {
		return err
	}
	if runtime.GOOS == "windows" {
		return extractZip(tmp.Name(), destDir)
	}
	return extractTarGz(tmp.Name(), destDir)
}

func logWrite(f *os.File, msg string) {
	if f != nil {
		f.WriteString(msg + "\n")
	}
}

func extractZip(src, dest string) error {
	data, err := os.ReadFile(src)
	if err != nil {
		return err
	}
	r, err := zip.NewReader(bytes.NewReader(data), int64(len(data)))
	if err != nil {
		return err
	}
	for _, f := range r.File {
		name := stripTopLevel(f.Name)
		if name == "" {
			continue
		}
		target := filepath.Join(dest, filepath.FromSlash(name))
		if f.FileInfo().IsDir() {
			os.MkdirAll(target, 0755)
			continue
		}
		os.MkdirAll(filepath.Dir(target), 0755)
		out, err := os.OpenFile(target, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, f.Mode())
		if err != nil {
			return err
		}
		rc, _ := f.Open()
		io.Copy(out, rc)
		rc.Close()
		out.Close()
	}
	return nil
}

func extractTarGz(src, dest string) error {
	f, err := os.Open(src)
	if err != nil {
		return err
	}
	defer f.Close()
	gz, err := gzip.NewReader(f)
	if err != nil {
		return err
	}
	defer gz.Close()
	tr := tar.NewReader(gz)
	for {
		hdr, err := tr.Next()
		if err == io.EOF {
			break
		}
		if err != nil {
			return err
		}
		name := stripTopLevel(hdr.Name)
		if name == "" {
			continue
		}
		target := filepath.Join(dest, filepath.FromSlash(name))
		switch hdr.Typeflag {
		case tar.TypeDir:
			os.MkdirAll(target, 0755)
		case tar.TypeSymlink:
			os.MkdirAll(filepath.Dir(target), 0755)
			os.Symlink(hdr.Linkname, target)
		case tar.TypeReg:
			os.MkdirAll(filepath.Dir(target), 0755)
			out, err := os.OpenFile(target, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, os.FileMode(hdr.Mode))
			if err != nil {
				return err
			}
			io.Copy(out, tr)
			out.Close()
		}
	}
	return nil
}

func stripTopLevel(p string) string {
	i := strings.Index(p, "/")
	if i < 0 {
		return ""
	}
	return p[i+1:]
}

func resolveInstallDir() string {
	home, _ := os.UserHomeDir()
	datahome := os.Getenv("XDG_DATA_HOME")
	switch runtime.GOOS {
	case "windows":
		if appdata := os.Getenv("APPDATA"); appdata != "" {
			return filepath.Join(appdata, ".cotsl")
		}
	case "darwin":
		return filepath.Join(home, "Library", "Application Support", ".cotsl")
	}
	if datahome != nil {
	    return filepath.Join(datahome, "cotsl")
	}
	return filepath.Join(home + "/.local/share/", "cotsl")
}

func fatalf(log *os.File, format string, args ...any) {
	msg := fmt.Sprintf("[CotSL] FATAL: "+format+"\n", args...)
	if log != nil {
		log.WriteString(msg)
	}
	os.Exit(1)
}