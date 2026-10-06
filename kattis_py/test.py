prices = {500: 500, 1000: 1000, 2000: 2000, 5250: 5000, 11000: 10000, 24000: 20000}

intak = int(input())

for i in prices:
    if intak <= i:
        print(prices[i])
        break