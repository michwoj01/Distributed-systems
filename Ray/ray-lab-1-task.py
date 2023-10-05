# based on the ray tutorial

# Ray enables arbitrary functions to be executed asynchronously on separate
# Python workers. These asynchronous Ray functions are called “tasks.” You
# can specify task's resource requirements in terms of CPUs, GPUs, and custom
# resources. These resource requests are used by the cluster scheduler to
# distribute tasks across the cluster for parallelized execution.

# importing framework

import cProfile
import os
import time
import logging

import numpy as np
import ray

# TOPIC 1 Tasks Parallel Pattern
# Ray converts decorated functions with @ray.remote into stateless tasks,
# scheduled anywhere on a Ray node's worker in the cluster.
# Where they will be executed (and by whom), you don't have to worry about
# its details. All that is taken care for you. Nor do you have to reason
# about it — all that burden is Ray's job. You simply take your existing
# Python functions and covert them into distributed stateless
# Ray Tasks: as simple as that!

# Example 1: Serial vs Parallelism
# There are a few key differences between the original function and the
# decorated one:
#
# a) Invocation: The regular version is called with regular_function(),
# whereas the remote version is called with remote_function.remote().
# Keep this pattern in mind for all Ray remote execution methods.
#
# b) Mode of execution and return values: regular_function executes
# synchronously and returns the result of the function as the value 1
# (in our case), whereas remote_function immediately returns an ObjectID
# (a future) and then executes the task in the background on a remote
# worker process. The result of the future is obtained by calling ray.get
# on the ObjectID. This is a blocking function.

# A regular Python function.


def regular_function():
    time.sleep(1)
    return 1

# A Ray remote function.


@ray.remote
def remote_function():
    time.sleep(1)
    return 1

# Let's launch a Ray cluster on our local machine. You can also connect to the cluster


if ray.is_initialized:
    ray.shutdown()
ray.init(logging_level=logging.ERROR)
# ray.init(address='auto', ignore_reinit_error=True, logging_level=logging.ERROR)


# Let's invoke the regular function
assert regular_function() == 1

# Let's invoke the remote function.
obj_ref = remote_function.remote()
print(obj_ref)

assert ray.get(obj_ref) == 1

# Serial execution in Python with no parallelism
#
# Invocations of regular_function in a comprehension loop happens serially:

# These are executed one at a time, back-to-back, in a list comprehension
results = [regular_function() for _ in range(10)]
assert sum(results) == 10

# Parallel execution in Python with Ray
#
# Invocations of remote_function in a loop happen asynchronously and in parallel:

# Executing these functions, in comprehension list, happens at the same time in the background,
# and we get the results using ray.get.

results = [remote_function.remote() for _ in range(10)]
assert sum(ray.get(results)) == 10

# Define a function as a Ray task to read an array


@ray.remote
def read_array(fn: str) -> np.array:
    arr = np.loadtxt(fn, comments="#", delimiter=",", unpack=False)
    return arr.astype('int')

# Define a function as a Ray task to add two np arrays return the sum


@ray.remote
def add_array(arr1: np.array, arr2: np.array) -> np.array:
    return np.add(arr1, arr2)

# Define a function as a Ray task to sum the contents of an np array


@ray.remote
def sum_array(arr1: np.array) -> int:
    return np.sum(arr1)

# Now let's execute our tasks. For now we will run Ray locally on our
# desktop or on a single node, with potential access to utilize all
# the available cores when necessary.

# Ray executes immediately and returns an object reference ObjectRef
# as a future. This enables Ray to parallelize tasks and execute them
# asynchronously.

# Read both arrays.
# Use the func_name.remote(args) extention to invoke a remote Ray Task


obj_ref_arr1 = read_array.remote(os.path.abspath("lab3/data/file_1.txt"))
print(f"array 1: {obj_ref_arr1}")

obj_ref_arr2 = read_array.remote(os.path.abspath("lab3/data/file_2.txt"))
print(f"array 2: {obj_ref_arr2}")


# Add both arrays
# Let's add our two arrays by calling the remote method. Note: We are sending
# Ray ObjectRef references as arguments. Those arguments will be resolved inline
# and fetched from owner's object store. That is, the cluster node that creates
# the ObjectRef owns the meta data associated and stores it in its object store.

# Ray scheduler is aware of where these object references reside or who owns them,
# so it will schedule this remote task on node on the worker process for data locality.


result_obj_ref = add_array.remote(obj_ref_arr1, obj_ref_arr2)
print(result_obj_ref)

# Fetch the result

# This will task if not finished will block during .get(object_ref)

result = ray.get(result_obj_ref)
print(f"Result: add arr1 + arr2: \n {result}")

# Add the array elements within an np.array and get the sum. Note that we are
# sending ObjRefs as arguments to the function. Ray will resolve or fetch the
# value of these arrays.

sum_1 = ray.get(sum_array.remote(obj_ref_arr1))
sum_2 = ray.get(sum_array.remote(obj_ref_arr2))

print(f'Sum of arr1: {sum_1}')
print(f'Sum of arr2: {sum_2}')

# Example 3: Generating Fibonnaci series
# Let's define two functions: one runs locally or serially, the other runs on
# a Ray cluster (local or remote)

# Function for local execution


def generate_fibonacci(sequence_size):
    fibonacci = []
    for i in range(0, sequence_size):
        if i < 2:
            fibonacci.append(i)
            continue
        fibonacci.append(fibonacci[i-1]+fibonacci[i-2])
    return len(fibonacci)

# Function for remote Ray task with just a wrapper


@ray.remote
def generate_fibonacci_distributed(sequence_size):
    return generate_fibonacci(sequence_size)


# Get the number of cores
os.cpu_count()

# Normal Python in a single process


def run_local(sequence_size):
    results = [generate_fibonacci(sequence_size)
               for _ in range(os.cpu_count())]
    return results


print('local run')
cProfile.run("run_local(100000)")


# Distributed on a Ray cluster


def run_remote(sequence_size):
    results = ray.get([generate_fibonacci_distributed.remote(
        sequence_size) for _ in range(os.cpu_count())])
    return results


print('remote run')
cProfile.run("run_remote(100000)")

ray.shutdown()
