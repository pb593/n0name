import matplotlib.pyplot as plt


if __name__ == "__main__":

    Ns = [2, 5, 10]
    col = ['r', 'g', 'b', 'k']

    for i in range(len(Ns)):
        N = Ns[i]
        with open('mem_vs_time_(%d,chat_heap).txt' % N, 'r') as f:
            vals = [float(row) for row in f]
            plt.plot(vals, color=col[i], label=("N = %d" % N))

    plt.legend(loc='lower right')
    plt.xlabel("Time / s")
    plt.ylabel("Memory consumption / MB")
    plt.title("Memory consumption over time (Patching period = 3s)")
    #plt.ylim(30, 120)s
    plt.show()