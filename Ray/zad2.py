import ray
import numpy as np
import cProfile
import logging

# Excercises 2.1) Create large lists and python dictionaries,
# put them in object store. Write a Ray task to process them.

ray.init(address="ray://localhost:10001",ignore_reinit_error=True, logging_level=logging.ERROR)



@ray.remote
def median_median(arr):
    n = len(arr)
    if n % 2 == 1:
        k = (n + 1) // 2
    else:
        k = n // 2

    if n <= 5:
        return median.remote(arr)

    medians = []
    for i in range(0, n, 5):
        group = arr[i:i+5]
        medians.append(median.remote(group))

    pivot = median_median.remote(medians)

    lows = [x for x in arr if x < pivot]
    highs = [x for x in arr if x > pivot]
    pivots = [x for x in arr if x == pivot]

    if k <= len(lows):
        return median_median.remote(lows)
    elif k > len(lows) + len(pivots):
        return median_median.remote(highs)
    else:
        return pivot


@ray.remote
def median(lst):
    n = len(lst)
    mid = n // 2

    for i in range(mid + 1):
        min_idx = i
        for j in range(i+1, n):
            if lst[j] < lst[min_idx]:
                min_idx = j
        lst[i], lst[min_idx] = lst[min_idx], lst[i]

    if n % 2 == 0:
        return (lst[mid - 1] + lst[mid]) / 2.0
    else:
        return lst[mid]


@ray.remote
def increment(arr):
    for el in arr:
        el += 1


@ray.remote
def decrement(dict):
    for el in dict:
        el -= 1


def process_arrays():
    results = ray.get(
        [increment.remote(
            ray.put(
                [np.random.randint(0, 10000) for _ in range(10000)]
            )
        ) for _ in range(10)])
    return results


def process_dicts():
    results = ray.get(
        [decrement.remote(
            ray.put(
                {key: np.random.randint(0, 10000) for key in range(10000)}
            )
        ) for _ in range(10)])
    return results


print('start array task')
cProfile.run("process_arrays()")


print('start dict task')
cProfile.run("process_dicts()")