service Calculator {
   double countMedian(1:Person person)
}

struct Person {
  1: string name
  2: optional i32 id;
  3: optional string email;
  4: optional list<double> taxes
}