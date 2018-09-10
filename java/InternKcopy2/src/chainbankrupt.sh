#!/bin/bash
set -ex
script_dir=$(cd $(dirname $BASH_SOURCE); pwd)
java -cp $script_dir ChainBankruptcy.Main $1 $2 $3 $4 $5 $6 $7 $8 $9 $10

#入力ファイル名
#$1:/Users/ryohamawaki/chainbankruptcy/java/InternKcopy2/result/balancesheet/balancesheet_sh.txt

#出力ファイル名
#$2:BSCAR.png

R --vanilla --slave  --args balancesheet_sh.txt BSCAR.png $1 $10 $11   < $script_dir/../result/balancesheet/BSCAROutput.R
