from math import sqrt
from fastapi import Depends, FastAPI, Form, HTTPException, Request, Response, status
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.security import OAuth2PasswordRequestForm
from fastapi_login import LoginManager
import uvicorn
import httpx
import os
import json


app = FastAPI()

SECRET = "secret"


class NotAuthenticatedException(Exception):
    pass


@ app.exception_handler(NotAuthenticatedException)
async def unicorn_exception_handler(request: Request, exc: NotAuthenticatedException):
    return RedirectResponse(url="/", status_code=230)


manager = LoginManager(SECRET, token_url="/login",
                       use_cookie=True, cookie_name="some-name", custom_exception=NotAuthenticatedException)

DB = {"michal": {"password": "password"}}


@manager.user_loader()
def load_user(username: str):
    user = DB.get(username)
    return user


@app.get("/", response_class=HTMLResponse)
def loginwithCreds():
    with open("lab2/login.html") as f:
        return HTMLResponse(content=f.read(), status_code=200)


@app.post("/login")
def login(request: Request, data: OAuth2PasswordRequestForm = Depends()):
    username = data.username
    password = data.password
    user = load_user(username)
    if not user:
        raise HTTPException(
            status_code=409, detail="Incorrect username")
    elif password != user['password']:
        raise HTTPException(
            status_code=410, detail="Incorrect password")
    access_token = manager.create_access_token(data={"sub": username})
    resp = RedirectResponse(url="/main", status_code=status.HTTP_302_FOUND)
    manager.set_cookie(resp, access_token)
    return resp


@app.get("/main", response_class=HTMLResponse)
async def main_site_form(_=Depends(manager)):
    with open("lab2/main.html") as f:
        return HTMLResponse(content=f.read(), status_code=200)


class UnknownDriverException(Exception):
    def __init__(self, name: str):
        self.name = name


@ app.exception_handler(UnknownDriverException)
async def unicorn_exception_handler(request: Request, exc: UnknownDriverException):
    with open("lab2/error.html") as f:
        html_content = f.read().format(
            "Check if {} really was driving those days".format(exc.name))
    return HTMLResponse(content=html_content, status_code=418)


class FootballApiException(Exception):
    def __init__(self, name: str):
        self.name = name


@ app.exception_handler(FootballApiException)
async def football_exception_handler(request: Request, exc: FootballApiException):
    with open("lab2/error.html") as f:
        html_content = f.read().format(
            "Whoah, looks what Football API has send me: \n".format(exc.name))
    return HTMLResponse(content=html_content, status_code=420)


@ app.exception_handler(httpx.ConnectTimeout)
async def football_exception_handler(request: Request, exc: httpx.ConnectTimeout):
    with open("lab2/error.html") as f:
        html_content = f.read().format("Timeout happened :((")
    return HTMLResponse(content=html_content, status_code=422)


@ app.post("/driver", response_class=HTMLResponse)
async def send_data_params(name: str = Form(), year: int = Form()):
    f1_url = "https://ergast.com/api/f1/{}/drivers/{}/qualifying.json".format(
        year, name)
    football_url = "https://v3.football.api-sports.io/standings"
    with open('lab2/env.json') as f:
        headers = json.load(f)
    params = {"league": '39', "season": year - 1}

    async with httpx.AsyncClient() as client:
        f1_response = await client.get(url=f1_url)
        football_response = await client.get(url=football_url, headers=headers, params=params)

    if f1_response.json()["MRData"]["total"] == '0':
        raise UnknownDriverException(name=name)

    f1_info = get_f1_data(f1_response)
    football_info = get_football_data(football_response, year)

    with open("lab2/result.html") as f:
        html_content = f.read().format(name, year, *f1_info, football_info)
    return HTMLResponse(content=html_content, status_code=200)


def get_f1_data(r: Response):
    data = r.json()["MRData"]["RaceTable"]["Races"]
    times = []
    poles = 0
    for race in data:
        qualifications = race["QualifyingResults"][0]
        if 'Q1' in qualifications and qualifications['Q1'] != '':
            times.append(qualifications["Q1"])
        if 'Q2' in qualifications and qualifications['Q2'] != '':
            times.append(qualifications["Q2"])
        if 'Q3' in qualifications and qualifications['Q3'] != '':
            times.append(qualifications["Q3"])
        if qualifications["position"] == "1":
            poles += 1
    calculated_times = {}
    avg = 0
    length = len(times)
    for time in times:
        time_elements = time.replace(".", ":").split(":")
        score = parse_to_number(time_elements)
        calculated_times[score] = time
        avg += score
    avg = round(avg / length, 2)
    dev = 0
    for score in calculated_times:
        dev += (score - avg)**2
    dev = parse_to_time(round(sqrt(dev), 2))
    med = calculated_times[sorted(calculated_times)[length//2]]
    avg = parse_to_time(avg)
    return [avg, med, dev, poles]


def get_football_data(r: Response, year: int):
    data = r.json()
    if data["errors"]:
        raise FootballApiException(data["errors"]["bug"])
    if data["results"] == 0:
        return "<h4>No Premier League results for that season</h4>"
    standings = []
    raw_data = data["response"][0]["league"]["standings"][0]
    for team in raw_data:
        standings.append(team["team"]["name"])
    with open("lab2/list.html") as f:
        return f.read().format(year, *standings)


def parse_to_number(time):
    score = 0
    score += int(time[0]) * 60000
    score += int(time[1]) * 1000
    score += int(time[2])
    return score


def parse_to_time(score):
    minutes = str(int(score // 60000))
    score %= 60000
    seconds = str(int(score // 1000))
    score %= 1000
    return minutes + ":" + seconds + "." + str(int(score))


if __name__ == "__main__":
    uvicorn.run("distributed:app", port=8080, log_level="info", reload=True)
