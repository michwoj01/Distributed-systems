# excercise 3
# 3.0 start remote cluster settings and observe actors in cluster
# a) make screenshot of dependencies
# 3.1. Modify the Actor class MethodStateCounter and add/modify methods that return the following:
# a) - Get number of times an invoker name was called
# b) - Get a list of values computed by invoker name
# c) - Get state of all invokers
# 3.2 Modify method invoke to return a random int value between [5, 25]
# 3.3 Take a look on implement parralel Pi computation
# based on https://docs.ray.io/en/master/ray-core/examples/highly_parallel.html
#
# Implement calculating pi as a combination of actor (which keeps the
# state of the progress of calculating pi as it approaches its final value)
# and a task (which computes candidates for pi)

import logging
import ray
import random
import numpy as np

ray.init(address="ray://localhost:10001",ignore_reinit_error=True, logging_level=logging.ERROR)


@ray.remote
class MethodStateCounter:

    def __init__(self):
        self._state = {}

    def invoke(self, invoker_name):
        self._state[invoker_name] = self._state.get(invoker_name, 0) + 1
        return random.randint(5, 25)

    def get_state(self):
        return self._state

    def get_state_by_invoker(self, invoker_name):
        return self._state.get(invoker_name, 0)

    def get_state_by_invoker_values(self, invoker_name):
        return self._state.get(invoker_name, [])


@ray.remote
def compute_pi_part(counter, num_samples):
    x = np.random.uniform(0, 1, size=num_samples)
    y = np.random.uniform(0, 1, size=num_samples)
    inside = (x**2 + y**2) < 1
    pi = 4 * inside.mean()
    counter.invoke.remote("compute_pi_part")
    return pi


counter = MethodStateCounter.remote()

futures = [compute_pi_part.remote(counter, 1000000) for _ in range(4)]
pi = sum(ray.get(futures)) / len(futures)
print("Pi is roughly", pi)

print("Counter state:", ray.get(counter.get_state.remote()))

print("Counter state for 'compute_pi_part':",
      ray.get(counter.get_state_by_invoker.remote("compute_pi_part")))
print("Counter state for 'compute_pi_part':",
      ray.get(counter.get_state_by_invoker_values.remote("compute_pi_part")))
