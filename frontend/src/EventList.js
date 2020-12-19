import React from 'react';
import Table from 'react-bootstrap/Table';
import PropTypes from 'prop-types';
import Alert from 'react-bootstrap/Alert';
import { makeFsm } from './utils';

const FsmState = makeFsm('Loading', 'Displaying', 'Failing');

export default class EventList extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      fsmState: FsmState.Loading,
      events: null,
    };
  }

  componentDidMount() {
    this.props.eventRepository
      .findAll()
      .then((events) => this.setState({ fsmState: FsmState.Displaying, events }))
      .catch(() => this.setState({ fsmState: FsmState.Failing }));
  }

  render() {
    let Details = () => <></>;

    if (this.state.fsmState.isFailing) {
      Details = () => <Alert variant="danger">Could not load events.</Alert>;
    } else if (this.state.fsmState.isLoading) {
      Details = () => <div>Loading...</div>;
    } else if (this.state.events.length === 0) {
      Details = () => <div>There are no events.</div>;
    } else {
      Details = () => (
        <Table responsive>
          <thead>
            <tr>
              {/* eslint-disable-next-line jsx-a11y/control-has-associated-label */}
              <th />
              <th>Title</th>
              <th>Host</th>
              <th>Date</th>
              <th>Guests</th>
              <th>Description</th>
            </tr>
          </thead>
          <tbody>
            {this.state.events.map((event) => (
              <tr key={event.id}>
                <td>
                  <a href={`/events/${event.id}`}>✎</a>
                </td>
                <td>{event.title}</td>
                <td>{event.host}</td>
                <td>{event.dateTime}</td>
                <td>Guests...</td>
                <td>{event.description}</td>
              </tr>
            ))}
          </tbody>
        </Table>
      );
    }

    return (
      <>
        <h2>My Events</h2>
        <Details />
      </>
    );
  }
}

EventList.propTypes = {
  eventRepository: PropTypes.shape({
    findAll: PropTypes.func.isRequired,
  }).isRequired,
};
