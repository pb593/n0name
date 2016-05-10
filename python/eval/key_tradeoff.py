import matplotlib.pyplot as plt

if __name__ == "__main__":

    latency_file = open('prop_delay/results/prop_delay_vs_PF(5)_no_seal.txt', 'r')
    traffic_file = open('traffic/results/pers_traffic_vs_PF(2,idle).txt', 'r')
    mem_file = open('memory/results/mem_vs_PF(5).txt', 'r')
    cpu_file = open('cpu/cpu_vs_pf(5).txt', 'r')

    plt.subplot(411)
    periods = []
    latencies = []
    for row in latency_file:
        tokens = row.strip().replace(' ', '').split(',')
        periods.append(float(tokens[0]))
        latencies.append(float(tokens[1]))

    plt.plot(periods, latencies)
    plt.xlabel('Patching period / s')
    plt.ylabel('Average message latency / s')


    plt.subplot(412)
    periods = []
    traffic = []
    for row in traffic_file:
        tokens = row.strip().replace(' ', '').split(',')
        print(tokens)
        periods.append(float(tokens[0]))
        traffic.append(float(tokens[1]) / 1000.0)

    plt.plot(periods, traffic)
    plt.xlabel('Patching period / s')
    plt.ylabel('Average traffic consumption / kBps')

    plt.subplot(413)
    periods = []
    mem_average = []
    mem_max = []
    for row in mem_file:
        tokens = row.strip().replace(' ', '').split(',')
        periods.append(float(tokens[0]))
        mem_average.append(float(tokens[1]))
        mem_max.append(float(tokens[2]))

    plt.plot(periods, mem_average, label='Average')
    plt.plot(periods, mem_max, '-', label='Maximum')
    plt.xlabel('Patching period / s')
    plt.ylabel('Memory footprint / MB')
    plt.legend()

    plt.subplot(414)
    periods = []
    cpu = []
    for row in cpu_file:
        tokens = row.strip().replace(' ', '').split(',')
        periods.append(float(tokens[0]))
        cpu.append(float(tokens[1]))

    plt.plot(periods, cpu)
    plt.xlabel('Patching period / s')
    plt.ylabel('Average CPU usage / %')


    plt.show()
