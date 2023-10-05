# In Ray, tasks and actors create and compute on objects. We refer to these
# objects as remote objects because they can be stored anywhere in a Ray
# cluster, and we use object refs to refer to them. Remote objects are cached
# in Ray’s distributed shared-memory object store, and there is one object
# store per node in the cluster. In the cluster setting, a remote object can
# live on one or many nodes, independent of who holds the object ref(s).

# Remote Objects reside in a distributed shared-memory object store.

# Objects are immutable and can be accessed from anywhere on the cluster, as
# they are stored in the cluster shared memory. An object ref is essentially
# a pointer or a unique ID that can be used to refer to a remote object without
# seeing its value. If you’re familiar with futures in Python, Java or Scala,
# Ray object refs are conceptually similar.

# In general, small objects are stored in their owner’s in-process store (<=100KB),
# while large objects are stored in the distributed object store. This decision is
# meant to reduce the memory footprint and resolution time for each object. Note
# that in the latter case, a placeholder object is stored in the in-process store to
# indicate the object has been promoted to shared memory.

# In the case if there is no space in the shared-memory, objects are spilled over to
# disk. But the main point here is that shared-memory allows zero-copy access to
# processes on the same worker node.

# Object references as futures pattern

from ray.exceptions import GetTimeoutError
import cProfile
import time
import logging
import numpy as np
import ray

if ray.is_initialized:
    ray.shutdown()
ray.init(logging_level=logging.ERROR)
# ray.init(address='auto', ignore_reinit_error=True, logging_level=logging.ERROR)


# Remote Objects example
num_list = [23, 42, 93]

# returns an objectRef
obj_ref = ray.put(num_list)
print(obj_ref)

# Then retrieve the value of this object reference.
# Small objects are resolved by copying them directly from the owner’s
# in-process store. For example, if the owner calls ray.get, the system
# looks up and deserializes the value from the local in-process store. For
# larger objects greater than 100KB, they will be stored in the distributed
# object store.

val = ray.get(obj_ref)
print(val)

# You can gather the values of multiple object references in parallel using a
# list comprehension:

# 1. Each value is put in the object store and its ObjRefID is immediately returned
# 2. The comprehension constructs a list of ObjRefIDs for each element in the loop
# 3. A final get(list_obj_refs) is invoked to fetch the list

results = ray.get([ray.put(i) for i in range(10)])
print(results)

# Passing Objects by Reference

# Ray object references can be freely passed around a Ray application. This means
# that they can be passed as arguments to tasks, actor methods, and even stored
# in other objects. Objects are tracked via distributed reference counting, and their
# data is automatically freed once all references to the object are deleted.

# Define a Task


@ray.remote
def echo(x):
    print(f"current value of argument x: {x}")


# Define some variables
x = list(range(10))
obj_ref_x = ray.put(x)
y = 25

# Pass-by-value
# Send the object to a task as a top-level argument. The object will
# be de-referenced automatically, so the task only sees its value.

# send y as value argument
echo.remote(y)

# send a an object reference
# note that the echo function deferences it
echo.remote(obj_ref_x)

# Pass-by-reference
# When a parameter is passed inside a Python list or as any other data structure,
# the object ref is preserved, meaning it's not de-referenced. The object data
# is not transferred to the worker when it is passed by reference, until ray.get()
# is called on the reference.

# You can pass by reference in two ways:
# 1. as a dictionary .remote({"obj": obj_ref_x})
# 2. as list of objRefs .remote([obj_ref_x])

x = list(range(20))
obj_ref_x = ray.put(x)
# Echo will not automaticall de-reference it
echo.remote({"obj": obj_ref_x})

echo.remote([obj_ref_x])

# What about long running tasks?

# Sometimes, you may have tasks that are long running, past their expected
# times due to some problem, maybe blocked on accessing a variable in the
# object store. How do you exit or terminate it? Use a timeout!
#
# Now let's set a timeout to return early from an attempted access of a remote
# object that is blocking for too long...


@ray.remote
def long_running_function():
    time.sleep(10)
    return 42

# You can control how long you want to wait for the task to finish


def time_out_funtion():
    obj_ref = long_running_function.remote()
    try:
        ray.get(obj_ref, timeout=6)
    except GetTimeoutError:
        print("`get` timed out")


print('start task')
cProfile.run("time_out_funtion()")


ray.shutdown()
