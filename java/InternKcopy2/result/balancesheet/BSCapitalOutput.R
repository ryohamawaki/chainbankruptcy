table <- read.table("balancesheet.txt")

num_node <- 100
time_step <- 10 + 1
num_trial <- 10
num_each_block <- num_node * time_step


pdf("BSCapital10.pdf")
for( trial in 1:num_trial){
  
  x <- matrix(0, nrow = num_node, ncol = time_step)  
  y <- matrix(0, nrow = num_node, ncol = time_step)  
  
  for( i in (num_each_block * (trial - 1) + 1):(num_each_block * trial) ){
    quotient <- ((((i-1)%/%num_node) + 1) - ((trial - 1) * time_step))
    for( j in 0:(num_node - 1) ){
      if(i%%num_node == 0){
        x[num_node, quotient] <- table[i,1]
        y[num_node, quotient] <- table[i,3]
      }
      if((i-1)%%num_node == j){
        x[j, quotient] <- table[i,1]
        y[j, quotient] <- table[i,3]
      }
    }
  }
  for(i in 1:num_node){
    plot(x[i, ], y[i, ], type = "l", ylim = c(20, 500))
  if(i != num_node) { par(new=T) }
  }
}
dev.off()