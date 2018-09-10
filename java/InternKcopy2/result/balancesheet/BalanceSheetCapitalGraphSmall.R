table <- read.table("smallbank.txt")

x <- matrix(0, nrow=90, ncol=11)  
y <- matrix(0, nrow=90, ncol=11)  

for( i in 1:990 ){
  quotient <- ((i-1)%/%90) + 1
  for( j in 0:89 ){
    if(i%%90 == 0){
      x[90, quotient] <- table[i,1]
      y[90, quotient] <- table[i,3]
    }
    if((i-1)%%90 == j){
      x[j, quotient] <- table[i,1]
      y[j, quotient] <- table[i,3]
    }
  }
}
plot(0,0)
for(i in 1:90){
  plot(x[i, ], y[i, ], type = "l", ylim = c(20, 80))
  par(new=T)
}