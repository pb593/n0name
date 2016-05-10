import matplotlib.pyplot as plt
import os
import numpy as np

def plot_alltraffic_vs_N():
    os.chdir('results')
    data_file = open('alltraffic_vs_N(18).txt', 'r')

    Ns = []
    traf_vals = []

    for row in data_file:
        tokens = row.replace(' ', '').split(',')
        N, traf = float(tokens[0]), float(tokens[1])

        Ns.append(N)
        traf_vals.append(traf / 1000.0)


    plt.plot(Ns, traf_vals, marker='o', label = "Patching period = 3s")
    plt.xlabel('Number of clique members')
    plt.ylabel('Total traffic in the clique / kBps')
    plt.title('Traffic in clique vs number of participants')
    plt.xlim(1, 19)
    plt.legend(loc="upper left")
    plt.show()
    data_file.close()


def plot_pers_traf_vs_PF():
    os.chdir('results')
    data_file = open('pers_traffic_vs_PF(2,idle).txt', 'r')

    periods = []
    traf_vals = []

    for row in data_file:
        tokens = row.replace(' ', '').split(',')
        patch_period, traf = float(tokens[0]), float(tokens[1])

        periods.append(patch_period / 1000.0) # convert to seconds
        traf_vals.append(traf / 1000.0) # account for both up and down link and conv to kB

    plt.plot(periods, traf_vals, marker='o', label="N=2")
    plt.xlabel('Inter-burst patching period / s')
    plt.ylabel('Average personal traffic  / kBps')
    plt.title('Traffic in clique vs inter-burst patching period')
    plt.legend()
    plt.show()
    data_file.close()


if __name__ == "__main__":
    plot_pers_traf_vs_PF()