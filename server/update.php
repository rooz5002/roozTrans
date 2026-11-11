<?php
// roozTrans self-update endpoint. Only the app (which knows the token) can use it.
$TOKEN_SHA256 = 'PUT_SHA256_OF_update.token_HERE';   // sha256sum update.token
$DIR = dirname(__DIR__, 2) . '/rooztrans-private/';   // <site>/public_html/rooztrans/ -> <site>/rooztrans-private/
if (!hash_equals($TOKEN_SHA256, hash('sha256', $_GET['t'] ?? ''))) { http_response_code(403); exit('forbidden'); }
header('Cache-Control: no-store');
if (isset($_GET['get'])) {
    // Range support so Android's DownloadManager can resume after a dropped connection.
    set_time_limit(0);
    $apk = $DIR . 'roozTrans.apk';
    $size = filesize($apk);
    $v = json_decode(@file_get_contents($DIR . 'version.json'), true);
    $etag = '"' . ($v['sha256'] ?? md5($size . filemtime($apk))) . '"';
    header('Content-Type: application/vnd.android.package-archive');
    header('Accept-Ranges: bytes');
    header('ETag: ' . $etag);
    $start = 0; $end = $size - 1;
    $ifRange = $_SERVER['HTTP_IF_RANGE'] ?? '';
    if (isset($_SERVER['HTTP_RANGE']) && ($ifRange === '' || $ifRange === $etag)
        && preg_match('/^bytes=(\d*)-(\d*)$/', trim($_SERVER['HTTP_RANGE']), $m)) {
        if ($m[1] === '' && $m[2] !== '') { $start = max(0, $size - (int)$m[2]); }
        else { $start = (int)$m[1]; if ($m[2] !== '') $end = min((int)$m[2], $size - 1); }
        if ($start > $end || $start >= $size) {
            http_response_code(416); header("Content-Range: bytes */$size"); exit;
        }
        http_response_code(206);
        header("Content-Range: bytes $start-$end/$size");
    }
    $len = $end - $start + 1;
    header('Content-Length: ' . $len);
    $fh = fopen($apk, 'rb'); fseek($fh, $start);
    while ($len > 0 && !feof($fh) && !connection_aborted()) {
        $chunk = fread($fh, min(1048576, $len)); echo $chunk; flush(); $len -= strlen($chunk);
    }
    fclose($fh); exit;
}
header('Content-Type: application/json');
readfile($DIR . 'version.json');
