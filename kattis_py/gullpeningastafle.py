n = int(input())

guess = [i for i in range(1, n + 1)]

print('?', ' '.join([str(i) for i in range(1, n + 1)]))

weight = int(input())

ans = weight - sum(guess) * n

print(f'! {ans}')