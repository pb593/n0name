import matplotlib.pyplot as plt


if __name__ == "__main__":

    Ns = [2, 5, 10]
    colors =['r', 'g', 'b']

    for i in range(len(Ns)):
        N = Ns[i]
        with open('cpu_vs_pf(%d).txt' % N, 'r') as f:
            usage = []
            period = []
            for row in f:
                tokens = row.strip().replace(" ", "").split(",")
                usage.append(float(tokens[1]))
                period.append(float(tokens[0]) / 1000.0)


            plt.plot(period, usage, marker = 'o', color=colors[i], label=("N=%d" % N))

    plt.title("Average percentage of CPU used vs patching period")
    plt.ylabel("Average CPU usage / %")
    plt.xlabel("Patching period / s")
    plt.legend()
    plt.xlim(0, 31)
    plt.show()