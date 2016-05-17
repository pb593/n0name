import matplotlib.pyplot as plt
import os
import numpy as np

def plot_delay_vs_period():

    Ns = [2, 5, 10]
    colors = ['r', 'g', 'b']

    os.chdir('results')

    for i in range(len(Ns)):
        N = Ns[i]
        data_file = open('prop_delay_vs_PF(%d)_no_seal.txt' % N, 'r')

        periods = []
        delays = []

        for row in data_file:
            tokens = row.replace(' ', '').split(',')
            period, delay = float(tokens[0]), float(tokens[1])

            periods.append(period / 1000.0)
            delays.append(delay)

        plt.plot(periods, delays, marker='o', color=colors[i], label=("N=%d" % N))
        data_file.close()

    plt.legend(loc="upper left")
    plt.xlim(0, 31)
    plt.xlabel('Inter-burst patching period / s')
    plt.ylabel('Average message latency / s')
    plt.title('Messaging latency vs patching period')
    plt.show()


def plot_delay_vs_N():
    #os.chdir('results/error_bars')
    data_file = open('latency_vs_N(3000)', 'r')

    Ns = []
    delays = []
    errors = []

    for row in data_file:
        tokens = row.replace(' ', '').split(':')
        N, data = float(tokens[0]), [float(d) for d in tokens[1].split(",")]
        error = 1.96 * np.std(data) / np.sqrt(float(len(data)))


        Ns.append(N)
        delays.append(np.mean(data))
        errors.append(error)


    plt.errorbar(Ns, delays, yerr= errors, marker='o')
    plt.xlabel('Clique size')
    plt.ylabel('Message latency / s')
    plt.title('Message latency vs clique size, with sealing')
    plt.xlim(1, 11)
    plt.ylim(3, 8.5)
    plt.show()
    data_file.close()


if __name__ == "__main__":

    plot_delay_vs_N()

