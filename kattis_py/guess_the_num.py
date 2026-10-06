num = 1000 // 2 + 1
top = 1000
bottom = 0

Correct = False

count = 0

while not Correct:
    print(num)    
    count += 1
    action = input()

    if action == 'lower':
        top = num - 1
        num = (top - bottom) // 2 + bottom
        continue

    elif action == 'higher':
        bottom = num + 1
        num = (top - bottom) // 2 + bottom
        continue

    Correct = True

