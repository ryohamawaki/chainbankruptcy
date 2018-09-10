table <- read.table("largebank.txt")

x <- matrix(0, nrow=10, ncol=11)  
y <- matrix(0, nrow=10, ncol=11)  

for( i in 1:110 ){
  quotient <- ((i-1)%/%10) + 1
  for( j in 0:9 ){
    if(i%%10 == 0){
      x[10, quotient] <- table[i,1]
      y[10, quotient] <- table[i,3]
    }
    if((i-1)%%10 == j){
      x[j, quotient] <- table[i,1]
      y[j, quotient] <- table[i,3]
    }
  }
}

for(i in 1:10){
  plot(x[i, ], y[i, ], type = "l", ylim = c(300, 700))
  par(new=T)
}