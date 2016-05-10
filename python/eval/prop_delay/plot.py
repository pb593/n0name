import matplotlib.pyplot as plt
import os

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
    os.chdir('results')
    data_file = open('prop_delay_vs_N(3000)__.txt', 'r')

    Ns = []
    delays = []

    for row in data_file:
        tokens = row.replace(' ', '').split(',')
        N, delay = float(tokens[0]), float(tokens[1])

        Ns.append(N)
        delays.append(delay)


    plt.plot(Ns, delays, marker='o')
    plt.xlabel('Clique size')
    plt.ylabel('Average message latency / s')
    plt.title('Average message latency vs clique size')
    plt.xlim(1, 11)
    plt.ylim(3.5, 7)
    plt.show()
    data_file.close()


if __name__ == "__main__":

    plot_delay_vs_period()

