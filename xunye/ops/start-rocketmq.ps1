param(
 [string]$RocketMqHome='C:\develop\rocketmq\rocketmq-all-5.3.0-bin-release',
 [string]$JavaHome='C:\develop\jdk8'
)
# 仅启动本项目的本地中间件；已有端口服务不接管、不停止。
$projectRoot=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$runtime=Join-Path $projectRoot '.runtime\rocketmq'
New-Item -ItemType Directory -Force -Path $runtime | Out-Null
function Test-LocalPort([int]$port) {
 $socket=[System.Net.Sockets.TcpClient]::new()
 try {return $socket.ConnectAsync('127.0.0.1',$port).Wait(500)} catch {return $false} finally {$socket.Dispose()}
}
function Start-LocalMq([string]$role,[string]$main,[string]$config,[int]$port) {
 if(Test-LocalPort $port){Write-Output "$role already listening on $port; preserved existing service.";return}
 $java=Join-Path $JavaHome 'bin\java.exe'
 if(!(Test-Path -LiteralPath $java)){throw "JDK not found: $java"}
 $args=@('-Xms128m','-Xmx256m','-Xmn64m',"-Drocketmq.home.dir=$RocketMqHome","-Duser.home=$runtime",'-cp',"$RocketMqHome/lib/*",$main,'-c',$config)
 $p=Start-Process -FilePath $java -ArgumentList $args -WindowStyle Hidden -WorkingDirectory $RocketMqHome -RedirectStandardOutput (Join-Path $runtime "$role.out.log") -RedirectStandardError (Join-Path $runtime "$role.err.log") -PassThru
 $p.Id | Set-Content (Join-Path $runtime "$role.pid")
 Write-Output "$role started, PID=$($p.Id), port=$port"
}
Start-LocalMq 'namesrv' 'org.apache.rocketmq.namesrv.NamesrvStartup' (Join-Path $PSScriptRoot 'rocketmq-namesrv.conf') 9876
Start-LocalMq 'broker' 'org.apache.rocketmq.broker.BrokerStartup' (Join-Path $PSScriptRoot 'rocketmq-broker.conf') 10911
