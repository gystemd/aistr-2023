import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

input = open("../logs/assignment1/Problem11.txt").read().split("\n")
error_input = list(filter(lambda i: "error_" in i, input))
# error_input2 = error_input
# print(list(error_input))
error_codes = map(lambda i: i.split(':')[2], error_input)
print(set(error_codes))

dict = {}
unique_found = 0
found = []
for el in list(error_input):
    time = el.split(' : ')[0]
    error = el.split(':')[2]
    if not error in found:
        found.append(error)
        unique_found = unique_found + 1
        dict[time] = unique_found

# print(error_input.pop().split(':')[0])
# print(dict)
time_stamps = [i for i in range(1, int(error_input.pop().split(':')[0]))]
counts =  []


# print(time_stamps[:-1])
last = 0
for el in time_stamps:
    try:
        last = dict[str(el)]
        counts.append(last)
    except KeyError:
        counts.append(last)
        # continue

# print(time_stamps)
# print(counts)
plt.title("Error convergence Problem 11")
plt.plot(time_stamps, counts, "--")
plt.xlabel("Time (ms)")
plt.ylabel("Unique errors triggered")
plt.show()

