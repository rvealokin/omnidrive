# OmniDrive

OmniDrive is a web-application which consolidates all your cloud storages in a single place and views them all as single
filesystem. You can perform all common filesystem operations on your files without any effort - for example you can copy or move file from one storage to another with a couple clicks.

## How does it work

## Uploading files with cUrl

Copy a file from Google Drive to Yandex.Disk (note that you have to use file ID in order to identify file on Google Drive):
```
curl -X POST --data '{"from": "google://0BwxVfUACl1RvMWplN0NnR3V3aDQ", "to": "yandex:///igor-test-yandex.jpg"}' http://localhost:9090/newcopy
```

Copy a file from Yandex.Disk to Google Drive:
```
curl -X POST --data '{"from": "yandex:///igor-test-yandex.jpg", "to": "google://"}' http://localhost:9090/newcopy
```

Copy a file from Yandex.Disk to local file:
```
curl -X POST --data '{"from": "yandex:///igor-test-yandex.jpg", "to": "file:///Users/inikolaev/igor-test-yandex-locacal.jpg"}' http://localhost:9090/newcopy
```

Copy a file from Yandex.Disk to local directory:
```
curl -X POST --data '{"from": "yandex:///igor-test-yandex.jpg", "to": "file:///Users/inikolaev"}' http://localhost:9090/newcopy
```

