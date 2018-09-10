args <- commandArgs(trailingOnly = T)
table <- read.table(args[1])

num_node <- 100
time_step <- 10 + 1
num_trial <- as.integer(args[3])
num_each_block <- num_node * time_step
kind_of_item <- 10
#試行回数と何の表を出力したいかを書く
#01 時間
#02 総資産
#03 市場性資産
#04 貸出金
#05 現金
#06 自己資本
#07 預金
#08 借入金
#09 持ち株数
#10 自己資本比率

#名前を変える。以上
png(args[2],height=1440, width=1440, res=216)
for( trial in 1:num_trial){
  
  x <- matrix(0, nrow = num_node, ncol = time_step)  
  y <- matrix(0, nrow = num_node, ncol = time_step)  
  
  for( i in (num_each_block * (trial - 1) + 1):(num_each_block * trial) ){
    quotient <- ((((i-1)%/%num_node) + 1) - ((trial - 1) * time_step))
    for( j in 0:(num_node - 1) ){
      if(i%%num_node == 0){
        x[num_node, quotient] <- table[i,1]
        y[num_node, quotient] <- table[i,kind_of_item]
      }
      if((i-1)%%num_node == j){
        x[j, quotient] <- table[i,1]
        y[j, quotient] <- table[i,kind_of_item]
      }
    }
  }
  for(i in 1:num_node){
    min_y <- min(y)
    plot(x[i, ], y[i, ], type = "l", ylim = c(min_y, 1.0), xlab ="Step", ylab = "CapitalAssetsRate")
    if(i != num_node) { par(new=T) }
  }
}
dev.off()