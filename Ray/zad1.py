import ray
import numpy as np
import cProfile
import os
import logging

# Excercises 1.1)Try using local bubble sort and remote bubble sort,
# show difference

ray.init(address="ray://localhost:10001",ignore_reinit_error=True, logging_level=logging.ERROR)


def bubble_sort(size: int):
    arr = [np.random.randint(0, size) for _ in range(size)]
    for i in range(size):
        for j in range(i+1, size):
            if arr[i] > arr[j]:
                arr[i], arr[j] = arr[j], arr[i]


def serial_bubblesort(size: int):
    results = [bubble_sort(size)
               for _ in range(os.cpu_count())]
    return results


print('local run')
cProfile.run("serial_bubblesort(10000)")


@ray.remote
def distributed_bubble_sort(size: int):
    bubble_sort(size)


def parallel_bubblesort(size: int):
    results = ray.get([distributed_bubble_sort.remote(size)
                       for _ in range(os.cpu_count())])
    return results


print('remote run')
cProfile.run("parallel_bubblesort(10000)")

# local sort on predefined took 13s, when remote sort took 9s (second time 7.5s vs 5.6s)

# local sort on random arrays on all CPUs took 66.5s vs 19.3s remotely with Ray
