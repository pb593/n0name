import matplotlib.pyplot as plt
import numpy as np

if __name__ == "__main__":

    Ns = [5, 10]
    colors =['r', 'g', 'b']

    for i in range(len(Ns)):
        N = Ns[i]
        with open('cpu_vs_pf(%de).txt' % N, 'r') as f:
            usage = []
            period = []
            error = []
            for row in f:
                tokens = row.strip().replace(" ", "").split(":")
                period.append(float(tokens[0]) / 1000.0)

                data = [float(r) for r in tokens[1].split(",")]
                usage.append(np.mean(data))
                error.append(1.96 * np.std(data) / np.sqrt(float(len(data))))


            plt.errorbar(period, usage, yerr=error, marker = 'o', color=colors[i], label=("N=%d" % N))

    plt.title("Average percentage of CPU used vs patching period")
    plt.ylabel("Average CPU usage / %")
    plt.xlabel("Patching period / s")
    plt.legend()
    plt.xlim(0, 31)
    plt.show()