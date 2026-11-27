# 只停止本项目启动脚本记录且命令行仍匹配的进程，避免PID复用时误停其他程序。
$projectRoot=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$runtime=Join-Path $projectRoot '.runtime\rocketmq'
foreach($role in @('broker','namesrv')) {
 $pidFile=Join-Path $runtime "$role.pid"
 if(!(Test-Path -LiteralPath $pidFile)){continue}
 $mqProcessId=[int](Get-Content -LiteralPath $pidFile -Raw)
 $mqProcess=Get-CimInstance Win32_Process -Filter "ProcessId=$mqProcessId"
 if($mqProcess -and $mqProcess.CommandLine.Contains($PSScriptRoot) -and $mqProcess.CommandLine.Contains('org.apache.rocketmq.')) {
  Stop-Process -Id $mqProcessId -ErrorAction Stop
  Write-Output "$role stopped: $mqProcessId"
 }elseif($mqProcess){Write-Warning "Process $mqProcessId did not match this project's broker; preserved."}
}
