import React from 'react';
import logo from './logo.svg';
import './App.css';
import Container from 'react-bootstrap/Container';
import EventList from './EventList';
import EventEditor from './EventEditor';
import {
  BrowserRouter as Router,
  Switch,
  Route,
  Link
} from "react-router-dom";

export default function App() {
  return (
    <Container>
      <header>
        <h1 className="display-1"><a href="/">Eventer</a></h1>
      </header>
      <main>
        <Container>
          <Router>
            <Link to="/events/new">Create New Event</Link>

            <Switch>
              <Route path="/events/new">
                <EventEditor/>
              </Route>
              <Route path="/">
                <EventList/>
              </Route>
            </Switch>
          </Router>
        </Container>
      </main>
    </Container>
  );
}

