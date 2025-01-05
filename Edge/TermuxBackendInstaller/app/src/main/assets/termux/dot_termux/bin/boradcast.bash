#!/data/data/com.termux/files/usr/bin/bash
#
# Usage:
#
#   boradcast.sh <broadcastName>  [--es <key1Str> <valueStr>]*
#   
#
# Tommy Chang
#
# TC 2020-04-20 (Mon)
set -e
MY_NAME=`basename $0`

#-----------------------
## 1.) Setup error reporting
#-----------------------
function err_report {
   echo "Fail @ [${MY_NAME}:${1}]"
   echo -e "$usageStr"
   exit 1
}
trap 'err_report $LINENO' ERR


#-----------------------
## 2.) Setup usage:
#-----------------------
read -r -d '' usageStr << EOM || true
\n
  Incorrect Usage:
    $0 $*

  Correct Usage:
    $0 [options]* <broadcast> 

  Required:
    <broadcast>  -- The name of the broadcast. eg. "com.example.TESTME"

  Options:
    -d | --debug : print debug info
    -h | --help : show help
    -e | --es  <key> <val>  : specify extras key-value data. eg. "id" "1223"

  Example:
    $0 "com.example.INIT" 
    $0 -e "id" "1234" -e "name" "Tommy" "com.example.RUN" 
\n
EOM
# am broadcast -a com.journaldev.broadcastreceiver.SOME_ACTION --es "extra" "2"


#-----------------------
## 3.) handle arguments:
#-----------------------
nRequired=1
pIdx=0
keys=()
vals=()
while [[ $1 ]]; do
    case "$1" in
	(-h | --help)
	    echo -e "$usageStr"
	    exit 0;;
	(-d | --debug)
            debug=1
            shift 1;;
	(-e | --es)
	    # cVar=$2
            keys=("${keys[@]}" "$2")
            values=("${values[@]}" "$3")
	    # echo "cVar is $cVar"
	    shift 3;;
	(*)
	    case $pIdx in
		(0) 		# 1st positional argument
		    broadcast=$1
		    pIdx=$(($pIdx+1)); shift;;
		(*)
		    echo -e "$usageStr"
		    exit 1
	    esac
	    ;;
    esac
done
if [[ ! $pIdx -eq $nRequired ]]; then
    echo -e "$usageStr"
    exit 1;
fi

cmdArray=(/system/bin/app_process / com.example.termuxam.Am broadcast -a $broadcast)
maxIdx=$(( ${#keys[@]} - 1))
for idx in $(seq 0 $maxIdx); do
    cmdArray=("${cmdArray[@]}" --es "${keys[$idx]}" "${values[$idx]}")
done

for idx in $(seq 0 $maxIdx); do
    cmd="$cmd --es \"${keys[$idx]}\" \"${values[$idx]}\""
done

#-----------------------
## 4.) verify arguments:
#-----------------------
function showArgs() {
   arr=("$@")
   for i in "${arr[@]}";
      do
          echo "$i"
      done
}

if [[ "$debug" == "1" ]]; then
    echo "Run with Param:"
    echo "  broadcast = $broadcast"
    echo "  keys      = ${keys[@]}"
    echo "  values    = ${values[@]}"
    echo "${cmdArray[@]}"
    showArgs "${cmdArray[@]}"
fi


#-----------------------
## 5.) Run the command:
#-----------------------
export CLASSPATH=/data/data/com.termux/files/usr/libexec/termux-am/am.apk
unset LD_LIBRARY_PATH LD_PRELOAD
exec "${cmdArray[@]}" > /dev/null

