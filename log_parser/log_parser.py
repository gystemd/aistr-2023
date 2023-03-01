import matplotlib.pyplot as plt


def parse_input(file):
    input = open(file).read().split("\n")
    error_input = list(filter(lambda i: "error_" in i, input))

    error_codes = map(lambda i: i.split('_')[1], error_input)
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
    counts = []

    # print(time_stamps[:-1])
    last = 0
    for el in time_stamps:
        try:
            last = dict[str(el)]
            counts.append(last)
        except KeyError:
            counts.append(last)
            # continue
    return (counts, time_stamps)


(countsDumb, time_stamps_dumb) = parse_input("../logs/assignment1/dumb/Problem15.txt")
(countsSmart, time_stamps_smart) = parse_input("../logs/assignment1/Problem15.txt")
# countsDumb = numpy.zeros(len(time_stamps_smart))
plt.title("Error convergence Problem 15")
plt.plot(time_stamps_dumb, countsDumb, "--", label="Random")
plt.plot(time_stamps_smart, countsSmart, "--", label="Smart")
plt.xlabel("Time (ms)")
plt.ylabel("Unique errors triggered")
plt.legend()
plt.show()
